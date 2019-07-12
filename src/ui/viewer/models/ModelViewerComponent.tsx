import { Button, InputNumber, Switch, Upload } from "antd";
import { UploadChangeParam } from "antd/lib/upload";
import { UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { model_viewer_store } from "../../../stores/ModelViewerStore";
import { AnimationSelectionComponent } from "./AnimationSelectionComponent";
import { ModelSelectionComponent } from "./ModelSelectionComponent";
import "./ModelViewerComponent.less";
import { get_model_renderer } from "../../../rendering/ModelRenderer";
import { RendererComponent } from "../../RendererComponent";

@observer
export class ModelViewerComponent extends Component {
    componentDidMount(): void {
        if (!model_viewer_store.current_model) {
            model_viewer_store.load_model(model_viewer_store.models[5]);
        }
    }

    render(): ReactNode {
        return (
            <div className="v-m-ModelViewerComponent">
                <Toolbar />
                <div className="v-m-ModelViewerComponent-main">
                    <ModelSelectionComponent />
                    <AnimationSelectionComponent />
                    <RendererComponent
                        renderer={get_model_renderer()}
                        on_will_unmount={model_viewer_store.pause_animation}
                    />
                </div>
            </div>
        );
    }
}

@observer
class Toolbar extends Component {
    state = {
        filename: undefined,
    };

    render(): ReactNode {
        return (
            <div className="v-m-ModelViewerComponent-toolbar">
                <Upload
                    accept=".nj, .njm, .xj, .xvm"
                    showUploadList={false}
                    onChange={this.load_file}
                    // Make sure it doesn't do a POST:
                    customRequest={() => false}
                >
                    <Button icon="file">{this.state.filename || "Open file..."}</Button>
                </Upload>
                {model_viewer_store.animation && (
                    <>
                        <Button
                            icon={model_viewer_store.animation_playing ? "pause" : "caret-right"}
                            onClick={model_viewer_store.toggle_animation_playing}
                        >
                            {model_viewer_store.animation_playing
                                ? "Pause animation"
                                : "Play animation"}
                        </Button>
                        <div className="group">
                            <span>Frame rate:</span>
                            <InputNumber
                                value={model_viewer_store.animation_frame_rate}
                                onChange={value =>
                                    model_viewer_store.set_animation_frame_rate(value || 0)
                                }
                                min={1}
                                step={1}
                            />
                        </div>
                        <div className="group">
                            <span>Frame:</span>
                            <InputNumber
                                value={model_viewer_store.animation_frame}
                                onChange={value =>
                                    model_viewer_store.set_animation_frame(value || 0)
                                }
                                step={1}
                            />
                            <span>/ {model_viewer_store.animation_frame_count}</span>
                        </div>
                    </>
                )}
                <div className="group">
                    <span>Show skeleton:</span>
                    <Switch
                        checked={model_viewer_store.show_skeleton}
                        onChange={value => (model_viewer_store.show_skeleton = value)}
                    />
                </div>
            </div>
        );
    }

    private load_file = (info: UploadChangeParam<UploadFile>) => {
        if (info.file.originFileObj) {
            this.setState({ filename: info.file.name });
            model_viewer_store.load_file(info.file.originFileObj as File);
        }
    };
}
