import { EulerOrder, Quat, quat_product } from "./quaternions";

test("euler_angles ZYX order", () => {
    for (let angle = 0; angle < 2 * Math.PI; angle += Math.PI / 360) {
        const x = Quat.euler_angles(angle, 0, 0, EulerOrder.ZYX);
        const y = Quat.euler_angles(0, angle, 0, EulerOrder.ZYX);
        const z = Quat.euler_angles(0, 0, angle, EulerOrder.ZYX);
        const q = quat_product(quat_product(z, y), x);
        const q2 = Quat.euler_angles(angle, angle, angle, EulerOrder.ZYX);

        expect(q.w).toBeCloseTo(q2.w, 5);
        expect(q.x).toBeCloseTo(q2.x, 5);
        expect(q.y).toBeCloseTo(q2.y, 5);
        expect(q.z).toBeCloseTo(q2.z, 5);
    }
});

test("euler_angles ZXY order", () => {
    for (let angle = 0; angle < 2 * Math.PI; angle += Math.PI / 360) {
        const x = Quat.euler_angles(angle, 0, 0, EulerOrder.ZXY);
        const y = Quat.euler_angles(0, angle, 0, EulerOrder.ZXY);
        const z = Quat.euler_angles(0, 0, angle, EulerOrder.ZXY);
        const q = quat_product(quat_product(z, x), y);
        const q2 = Quat.euler_angles(angle, angle, angle, EulerOrder.ZXY);

        expect(q.w).toBeCloseTo(q2.w, 5);
        expect(q.x).toBeCloseTo(q2.x, 5);
        expect(q.y).toBeCloseTo(q2.y, 5);
        expect(q.z).toBeCloseTo(q2.z, 5);
    }
});
