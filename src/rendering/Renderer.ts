import * as THREE from 'three';
import { Color, HemisphereLight, Intersection, Mesh, MeshLambertMaterial, MOUSE, Object3D, PerspectiveCamera, Plane, Raycaster, Scene, Vector2, Vector3, WebGLRenderer, Clock } from 'three';
import OrbitControlsCreator from 'three-orbit-controls';
import { get_area_collision_geometry, get_area_render_geometry } from '../bin_data/loading/areas';
import { Area, Quest, QuestEntity, QuestNpc, QuestObject, Section, Vec3 } from '../domain';
import { quest_editor_store } from '../stores/QuestEditorStore';
import { NPC_COLOR, NPC_HOVER_COLOR, NPC_SELECTED_COLOR, OBJECT_COLOR, OBJECT_HOVER_COLOR, OBJECT_SELECTED_COLOR } from './entities';

const OrbitControls = OrbitControlsCreator(THREE);

interface PickEntityResult {
    object: Mesh;
    entity: QuestEntity;
    grabOffset: Vector3;
    dragAdjust: Vector3;
    dragY: number;
    manipulating: boolean;
}

let renderer: Renderer | undefined;

export function getRenderer(): Renderer {
    if (!renderer) {
        renderer = new Renderer();
    }

    return renderer;
}

/**
 * Renders a quest area or an NJ/XJ model.
 */
export class Renderer {
    private renderer = new WebGLRenderer({ antialias: true });
    private camera: PerspectiveCamera;
    private controls: any;
    private raycaster = new Raycaster();
    private scene = new Scene();
    private quest?: Quest;
    private questEntitiesLoaded = false;
    private area?: Area;
    private objs: Map<number, QuestObject[]> = new Map(); // Objs grouped by area id
    private npcs: Map<number, QuestNpc[]> = new Map(); // Npcs grouped by area id
    private collisionGeometry = new Object3D();
    private renderGeometry = new Object3D();
    private objGeometry = new Object3D();
    private npcGeometry = new Object3D();
    private hoveredData?: PickEntityResult;
    private selectedData?: PickEntityResult;
    private model?: Object3D;
    private clock = new Clock();

    constructor() {
        this.renderer.domElement.addEventListener(
            'mousedown', this.onMouseDown);
        this.renderer.domElement.addEventListener(
            'mouseup', this.onMouseUp);
        this.renderer.domElement.addEventListener(
            'mousemove', this.onMouseMove);

        this.camera = new PerspectiveCamera(75, 1, 0.1, 5000);
        this.controls = new OrbitControls(
            this.camera, this.renderer.domElement);
        this.controls.mouseButtons.ORBIT = MOUSE.RIGHT;
        this.controls.mouseButtons.PAN = MOUSE.LEFT;

        this.scene.background = new Color(0x151C21);
        this.scene.add(new HemisphereLight(0xffffff, 0x505050, 1));
        this.scene.add(this.objGeometry);
        this.scene.add(this.npcGeometry);

        requestAnimationFrame(this.renderLoop);
    }

    get domElement(): HTMLElement {
        return this.renderer.domElement;
    }

    setSize(width: number, height: number) {
        this.renderer.setSize(width, height);
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
    }

    setQuestAndArea(quest?: Quest, area?: Area) {
        let update = false;

        if (this.area !== area) {
            this.area = area;
            update = true;
        }

        if (this.quest !== quest) {
            this.quest = quest;

            this.objs.clear();
            this.npcs.clear();

            if (quest) {
                for (const obj of quest.objects) {
                    const array = this.objs.get(obj.area_id) || [];
                    array.push(obj);
                    this.objs.set(obj.area_id, array);
                }

                for (const npc of quest.npcs) {
                    const array = this.npcs.get(npc.area_id) || [];
                    array.push(npc);
                    this.npcs.set(npc.area_id, array);
                }
            }

            update = true;
        }

        if (update) {
            this.updateGeometry();
        }
    }

    /**
     * Renders a generic Object3D.
     */
    setModel(model?: Object3D) {
        if (this.model !== model) {
            if (this.model) {
                this.scene.remove(this.model);
            }

            if (model) {
                this.setQuestAndArea(undefined, undefined);
                this.scene.add(model);
                this.resetCamera();
            }

            this.model = model;
        }
    }

    private updateGeometry() {
        this.scene.remove(this.objGeometry);
        this.scene.remove(this.npcGeometry);
        this.objGeometry = new Object3D();
        this.npcGeometry = new Object3D();
        this.scene.add(this.objGeometry);
        this.scene.add(this.npcGeometry);
        this.questEntitiesLoaded = false;

        this.scene.remove(this.collisionGeometry);

        if (this.quest && this.area) {
            const episode = this.quest.episode;
            const areaId = this.area.id;
            const variant = this.quest.area_variants.find(v => v.area.id === areaId);
            const variantId = (variant && variant.id) || 0;

            get_area_collision_geometry(episode, areaId, variantId).then(geometry => {
                if (this.quest && this.area) {
                    this.setModel(undefined);
                    this.scene.remove(this.collisionGeometry);

                    this.resetCamera();

                    this.collisionGeometry = geometry;
                    this.scene.add(geometry);
                }
            });

            get_area_render_geometry(episode, areaId, variantId).then(geometry => {
                if (this.quest && this.area) {
                    this.renderGeometry = geometry;
                }
            });
        }
    }

    private resetCamera() {
        this.controls.reset();
        this.camera.position.set(0, 800, 700);
        this.camera.lookAt(new Vector3(0, 0, 0));
    }

    private renderLoop = () => {
        this.controls.update();
        this.addLoadedEntities();

        if (quest_editor_store.animation_mixer) {
            quest_editor_store.animation_mixer.update(this.clock.getDelta());
        }

        this.renderer.render(this.scene, this.camera);
        requestAnimationFrame(this.renderLoop);
    }

    private addLoadedEntities() {
        if (this.quest && this.area && !this.questEntitiesLoaded) {
            let loaded = true;

            for (const object of this.quest.objects) {
                if (object.area_id === this.area.id) {
                    if (object.object3d) {
                        this.objGeometry.add(object.object3d);
                    } else {
                        loaded = false;
                    }
                }
            }

            for (const npc of this.quest.npcs) {
                if (npc.area_id === this.area.id) {
                    if (npc.object3d) {
                        this.npcGeometry.add(npc.object3d);
                    } else {
                        loaded = false;
                    }
                }
            }

            this.questEntitiesLoaded = loaded;
        }
    }

    private onMouseDown = (e: MouseEvent) => {
        const oldSelectedData = this.selectedData;
        const data = this.pickEntity(
            this.pointerPosToDeviceCoords(e));

        // Did we pick a different object than the previously hovered over 3D object?
        if (this.hoveredData && (!data || data.object !== this.hoveredData.object)) {
            (this.hoveredData.object.material as MeshLambertMaterial).color.set(
                this.getColor(this.hoveredData.entity, 'normal'));
        }

        // Did we pick a different object than the previously selected 3D object?
        if (this.selectedData && (!data || data.object !== this.selectedData.object)) {
            (this.selectedData.object.material as MeshLambertMaterial).color.set(
                this.getColor(this.selectedData.entity, 'normal'));
            this.selectedData.manipulating = false;
        }

        if (data) {
            // User selected an entity.
            (data.object.material as MeshLambertMaterial).color.set(this.getColor(data.entity, 'selected'));
            data.manipulating = true;
            this.hoveredData = data;
            this.selectedData = data;
            this.controls.enabled = false;
        } else {
            // User clicked on terrain or outside of area.
            this.hoveredData = undefined;
            this.selectedData = undefined;
            this.controls.enabled = true;
        }

        const selectionChanged = oldSelectedData && data
            ? oldSelectedData.object !== data.object
            : oldSelectedData !== data;

        if (selectionChanged) {
            quest_editor_store.setSelectedEntity(data && data.entity);
        }
    }

    private onMouseUp = () => {
        if (this.selectedData) {
            this.selectedData.manipulating = false;
            this.controls.enabled = true;
        }
    }

    private onMouseMove = (e: MouseEvent) => {
        const pointerPos = this.pointerPosToDeviceCoords(e);

        if (this.selectedData && this.selectedData.manipulating) {
            if (e.buttons === 1) {
                // User is dragging a selected entity.
                const data = this.selectedData;

                if (e.shiftKey) {
                    // Vertical movement.
                    // We intersect with a plane that's oriented toward the camera and that's coplanar with the point where the entity was grabbed.
                    this.raycaster.setFromCamera(pointerPos, this.camera);
                    const ray = this.raycaster.ray;
                    const negativeWorldDir = this.camera.getWorldDirection(new Vector3()).negate();
                    const plane = new Plane().setFromNormalAndCoplanarPoint(
                        new Vector3(negativeWorldDir.x, 0, negativeWorldDir.z).normalize(),
                        data.object.position.sub(data.grabOffset));
                    const intersectionPoint = new Vector3();

                    if (ray.intersectPlane(plane, intersectionPoint)) {
                        const y = intersectionPoint.y + data.grabOffset.y;
                        const yDelta = y - data.entity.position.y;
                        data.dragY += yDelta;
                        data.dragAdjust.y -= yDelta;
                        data.entity.position = new Vec3(
                            data.entity.position.x,
                            y,
                            data.entity.position.z
                        );
                    }
                } else {
                    // Horizontal movement accross terrain.
                    // Cast ray adjusted for dragging entities.
                    const { intersection, section } = this.pickTerrain(pointerPos, data);

                    if (intersection) {
                        data.entity.position = new Vec3(
                            intersection.point.x,
                            intersection.point.y + data.dragY,
                            intersection.point.z
                        );
                        data.entity.section = section;
                    } else {
                        // If the cursor is not over any terrain, we translate the entity accross the horizontal plane in which the entity's origin lies.
                        this.raycaster.setFromCamera(pointerPos, this.camera);
                        const ray = this.raycaster.ray;
                        // ray.origin.add(data.dragAdjust);
                        const plane = new Plane(
                            new Vector3(0, 1, 0),
                            -data.entity.position.y + data.grabOffset.y);
                        const intersectionPoint = new Vector3();

                        if (ray.intersectPlane(plane, intersectionPoint)) {
                            data.entity.position = new Vec3(
                                intersectionPoint.x + data.grabOffset.x,
                                data.entity.position.y,
                                intersectionPoint.z + data.grabOffset.z
                            );
                        }
                    }
                }
            }
        } else {
            // User is hovering.
            const oldData = this.hoveredData;
            const data = this.pickEntity(pointerPos);

            if (oldData && (!data || data.object !== oldData.object)) {
                if (!this.selectedData || oldData.object !== this.selectedData.object) {
                    (oldData.object.material as MeshLambertMaterial).color.set(
                        this.getColor(oldData.entity, 'normal'));
                }

                this.hoveredData = undefined;
            }

            if (data && (!oldData || data.object !== oldData.object)) {
                if (!this.selectedData || data.object !== this.selectedData.object) {
                    (data.object.material as MeshLambertMaterial).color.set(
                        this.getColor(data.entity, 'hover'));
                }

                this.hoveredData = data;
            }
        }
    }

    private pointerPosToDeviceCoords(e: MouseEvent) {
        const coords = new Vector2();
        this.renderer.getSize(coords);
        coords.width = e.offsetX / coords.width * 2 - 1;
        coords.height = e.offsetY / coords.height * -2 + 1;
        return coords;
    }

    /**
     * @param pointerPos - pointer coordinates in normalized device space
     */
    private pickEntity(pointerPos: Vector2): PickEntityResult | undefined {
        // Find the nearest object and NPC under the pointer.
        this.raycaster.setFromCamera(pointerPos, this.camera);
        const [nearestObject] = this.raycaster.intersectObjects(
            this.objGeometry.children
        );
        const [nearestNpc] = this.raycaster.intersectObjects(
            this.npcGeometry.children
        );

        if (!nearestObject && !nearestNpc) {
            return;
        }

        const objectDist = nearestObject ? nearestObject.distance : Infinity;
        const npcDist = nearestNpc ? nearestNpc.distance : Infinity;
        const intersection = objectDist < npcDist ? nearestObject : nearestNpc;

        const entity = intersection.object.userData.entity;
        // Vector that points from the grabbing point to the model's origin.
        const grabOffset = intersection.object.position
            .clone()
            .sub(intersection.point);
        // Vector that points from the grabbing point to the terrain point directly under the model's origin.
        const dragAdjust = grabOffset.clone();
        // Distance to terrain.
        let dragY = 0;

        // Find vertical distance to terrain.
        this.raycaster.set(
            intersection.object.position, new Vector3(0, -1, 0)
        );
        const [terrain] = this.raycaster.intersectObjects(
            this.collisionGeometry.children, true
        );

        if (terrain) {
            dragAdjust.sub(new Vector3(0, terrain.distance, 0));
            dragY += terrain.distance;
        }

        return {
            object: intersection.object as Mesh,
            entity,
            grabOffset,
            dragAdjust,
            dragY,
            manipulating: false
        };
    }

    /**
     * @param pointerPos - pointer coordinates in normalized device space
     */
    private pickTerrain(pointerPos: Vector2, data: PickEntityResult): { intersection?: Intersection, section?: Section } {
        this.raycaster.setFromCamera(pointerPos, this.camera);
        this.raycaster.ray.origin.add(data.dragAdjust);
        const terrains = this.raycaster.intersectObjects(
            this.collisionGeometry.children, true);

        // Don't allow entities to be placed on very steep terrain.
        // E.g. walls.
        // TODO: make use of the flags field in the collision data.
        for (const terrain of terrains) {
            if (terrain.face!.normal.y > 0.75) {
                // Find section ID.
                this.raycaster.set(
                    terrain.point.clone().setY(1000), new Vector3(0, -1, 0));
                const renderTerrains = this.raycaster
                    .intersectObjects(this.renderGeometry.children, true)
                    .filter(rt => rt.object.userData.section.id >= 0);

                return {
                    intersection: terrain,
                    section: renderTerrains[0] && renderTerrains[0].object.userData.section
                };
            }
        }

        return {};
    }

    private getColor(entity: QuestEntity, type: 'normal' | 'hover' | 'selected') {
        const isNpc = entity instanceof QuestNpc;

        switch (type) {
            default:
            case 'normal': return isNpc ? NPC_COLOR : OBJECT_COLOR;
            case 'hover': return isNpc ? NPC_HOVER_COLOR : OBJECT_HOVER_COLOR;
            case 'selected': return isNpc ? NPC_SELECTED_COLOR : OBJECT_SELECTED_COLOR;
        }
    }
}
