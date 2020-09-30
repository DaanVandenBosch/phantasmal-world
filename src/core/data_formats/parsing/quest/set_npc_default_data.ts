import { NpcType } from "./npc_types";

// TODO: set properties of friendly NPCs based on episode.
export function set_npc_default_data(type: NpcType, view: DataView): void {
    switch (type) {
        case NpcType.FemaleFat:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1872, true);
            view.setInt16(10, 5969, true);
            view.setInt16(68, -16432, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.FemaleMacho:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1876, true);
            view.setInt16(10, 5973, true);
            view.setInt16(68, -12528, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.FemaleTall:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1883, true);
            view.setInt16(68, -5504, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.MaleDwarf:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1873, true);
            view.setInt16(10, 5970, true);
            view.setInt16(68, -15456, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.MaleFat:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1882, true);
            view.setInt16(10, 5979, true);
            view.setInt16(68, -6528, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.MaleMacho:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1880, true);
            view.setInt16(10, 5977, true);
            view.setInt16(68, -8576, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.MaleOld:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1878, true);
            view.setInt16(10, 5975, true);
            view.setInt16(68, -10576, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.BlueSoldier:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1875, true);
            view.setInt16(10, 5972, true);
            view.setInt16(68, -13504, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.RedSoldier:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1874, true);
            view.setInt16(10, 5971, true);
            view.setInt16(68, -14480, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.Principal:
            view.setInt16(4, 1888, true);
            view.setInt16(10, 5985, true);
            view.setInt16(68, -384, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.Tekker:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1879, true);
            view.setInt16(10, 5976, true);
            view.setInt16(68, -9600, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.GuildLady:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1891, true);
            view.setInt16(10, 5988, true);
            view.setInt16(68, 11584, true);
            view.setInt16(70, 1835, true);
            break;
        case NpcType.Scientist:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1877, true);
            view.setInt16(10, 5974, true);
            view.setInt16(68, -11552, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.Nurse:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1884, true);
            view.setInt16(10, 5981, true);
            view.setInt16(68, -4480, true);
            view.setInt16(70, 1834, true);
            break;
        case NpcType.Irene:
            view.setInt16(4, 1889, true);
            view.setInt16(10, 5986, true);
            view.setInt16(68, 640, true);
            view.setInt16(70, 1835, true);
            break;
        case NpcType.ItemShop:
            view.setInt16(4, 8, true);
            view.setInt16(10, 6453, true);
            view.setInt16(68, 16560, true);
            view.setInt16(70, 1176, true);
            break;
        case NpcType.Nurse2:
            view.setInt16(4, 2330, true);
            view.setInt16(10, 6496, true);
            view.setInt16(68, -13280, true);
            view.setInt16(70, 1200, true);
            break;
        case NpcType.Hildebear:
            view.setInt16(4, -1, true);
            view.setInt16(8, 2, true);
            view.setInt16(10, -1, true);
            view.setFloat32(44, 1.000000238418579, true); // Scale x
            view.setInt16(68, 29968, true);
            view.setInt16(70, -29446, true);
            break;
        case NpcType.RagRappy:
            view.setInt16(8, 1, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 1072, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Monest:
            view.setFloat32(48, 5.000000953674316, true); // Start number
            view.setFloat32(52, 10.000004768371582, true); // Total number
            break;
        case NpcType.BarbarousWolf:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 6475, true);
            view.setInt16(68, 8576, true);
            view.setInt16(70, -29445, true);
            break;
        case NpcType.Booma:
            view.setFloat32(44, 0.30000001192092896, true); // Scale x
            view.setFloat32(48, 40.00001907348633, true); // Idle distance
            break;
        case NpcType.Gobooma:
            view.setInt16(8, 1, true);
            view.setInt16(10, -1, true);
            view.setFloat32(44, 0.30000001192092896, true); // Scale x
            view.setFloat32(48, 40.00001907348633, true); // Idle distance
            view.setInt16(68, 11600, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Gigobooma:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 6492, true);
            view.setInt16(68, -4000, true);
            view.setInt16(70, -29446, true);
            break;
        case NpcType.Dragon:
            view.setInt16(4, 1173, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 24624, true);
            view.setInt16(70, -29446, true);
            break;
        case NpcType.GrassAssassin:
            view.setInt16(8, 4, true);
            break;
        case NpcType.PoisonLily:
            view.setInt16(8, 4, true);
            break;
        case NpcType.NanoDragon:
            view.setInt16(8, 3, true);
            break;
        case NpcType.EvilShark:
            view.setInt16(8, 3, true);
            break;
        case NpcType.PalShark:
            view.setInt16(8, 3, true);
            break;
        case NpcType.GuilShark:
            view.setInt16(8, 4, true);
            break;
        case NpcType.PofuillySlime:
            view.setInt16(8, 4, true);
            break;
        case NpcType.PanArms:
            view.setInt16(8, 5, true);
            break;
        case NpcType.DeRolLe:
            view.setInt16(4, 1485, true);
            view.setInt16(6, 19, true); // Clone count
            view.setInt16(8, 12, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, -11088, true);
            view.setInt16(70, -29445, true);
            break;
        case NpcType.Dubchic:
            view.setInt16(4, 2626, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 7272, true);
            view.setInt16(68, -25504, true);
            view.setInt16(70, 561, true);
            break;
        case NpcType.Gilchic:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -1, true);
            view.setFloat32(44, 1.000000238418579, true); // Scale x
            view.setInt16(68, 5968, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Garanz:
            view.setInt16(4, -1, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 6319, true);
            view.setInt16(68, -26128, true);
            view.setInt16(70, 561, true);
            break;
        case NpcType.SinowBeat:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 6288, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.SinowGold:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -1, true);
            view.setFloat32(44, -1.000000238418579, true); // Scale x
            view.setInt16(68, 8048, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Canadine:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 8496, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Canane:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 7264, true);
            view.setInt16(70, -29444, true);
            break;
        case NpcType.Dubswitch:
            view.setInt16(4, 2626, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 7298, true);
            view.setInt16(68, -16736, true);
            view.setInt16(70, 561, true);
            break;
        case NpcType.VolOptPart1:
            view.setInt16(6, 35, true); // Clone count
            break;
        case NpcType.VolOptPart2:
            view.setInt16(8, 13, true);
            break;
        case NpcType.DarkFalz:
            view.setInt16(4, -1, true);
            view.setInt16(8, 14, true);
            view.setInt16(10, 7458, true);
            view.setInt16(68, 25008, true);
            view.setInt16(70, -29446, true);
            break;
        case NpcType.Hildebear2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 30745, true);
            view.setFloat32(44, 1, true); // Scale x
            view.setInt16(68, -7296, true);
            view.setInt16(70, -32759, true);
            break;
        case NpcType.RagRappy2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, -7401, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, 8201, true);
            break;
        case NpcType.Monest2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 4122, true);
            view.setFloat32(48, 3, true); // Start number
            view.setFloat32(52, 9, true); // Total number
            view.setInt16(68, -7296, true);
            view.setInt16(70, -12252, true);
            break;
        case NpcType.PoisonLily2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 26648, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, 8230, true);
            break;
        case NpcType.GrassAssassin2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 20761, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, 24595, true);
            break;
        case NpcType.Dimenian2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 28696, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, -4086, true);
            break;
        case NpcType.LaDimenian2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, 7449, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, -16367, true);
            break;
        case NpcType.SoDimenian2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, -1254, true);
            view.setFloat32(48, 100, true); // Idle distance
            view.setInt16(68, -7040, true);
            view.setInt16(70, 8372, true);
            break;
        case NpcType.DarkBelra2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 1, true);
            view.setInt16(10, -17895, true);
            view.setInt16(68, -7040, true);
            view.setInt16(70, -32642, true);
            break;
        case NpcType.BarbaRay:
            view.setInt16(4, -1, true);
            view.setInt16(8, 14, true);
            view.setInt16(10, 23572, true);
            view.setInt16(68, -2688, true);
            view.setInt16(70, 24576, true);
            break;
        case NpcType.SavageWolf2:
            view.setInt16(4, 11785, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -20711, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, 8250, true);
            break;
        case NpcType.BarbarousWolf2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -14056, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, -32650, true);
            break;
        case NpcType.PanArms2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -6632, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, -16251, true);
            break;
        case NpcType.Dubchic2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -3560, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, 16513, true);
            break;
        case NpcType.Gilchic2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -23272, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, -32654, true);
            break;
        case NpcType.Garanz2:
            view.setInt16(4, 27144, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, 27928, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, 24683, true);
            break;
        case NpcType.Dubswitch2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, -4840, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, -20363, true);
            break;
        case NpcType.Delsaber2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, 2841, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, 16513, true);
            break;
        case NpcType.ChaosSorcerer2:
            view.setInt16(4, -1, true);
            view.setInt16(8, 4, true);
            view.setInt16(10, 9754, true);
            view.setInt16(68, -7296, true);
            view.setInt16(70, -7963, true);
            break;
        case NpcType.GolDragon:
            view.setInt16(4, -19963, true);
            view.setInt16(8, 15, true);
            view.setInt16(10, -18411, true);
            view.setInt16(68, -3712, true);
            view.setInt16(70, 16555, true);
            break;
        case NpcType.SinowBerill:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 15896, true);
            view.setFloat32(44, 3, true); // Scale x
            view.setFloat32(52, -0.19999998807907104, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 47, true);
            break;
        case NpcType.SinowSpigell:
            view.setInt16(4, 880, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 7101, true);
            view.setInt16(68, -11584, true);
            view.setInt16(70, 1163, true);
            break;
        case NpcType.Merillia:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, 28439, true);
            view.setFloat32(44, 1, true); // Scale x
            view.setFloat32(52, -0.09999999403953552, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 16456, true);
            break;
        case NpcType.Meriltas:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, 30999, true);
            view.setFloat32(44, -1, true); // Scale x
            view.setFloat32(52, 0.09999999403953552, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 16456, true);
            break;
        case NpcType.Mericarol:
            view.setInt16(4, -1, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 30232, true);
            view.setFloat32(44, 0.19999998807907104, true); // Scale x
            view.setFloat32(52, 0.19999998807907104, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, -4016, true);
            break;
        case NpcType.Mericus:
            view.setInt16(4, 32010, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 3356, true);
            view.setFloat32(44, 0.19999998807907104, true); // Scale x
            view.setFloat32(52, 0.19999998807907104, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 28762, true);
            break;
        case NpcType.Merikle:
            view.setInt16(4, 32010, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 3868, true);
            view.setFloat32(44, 0.19999998807907104, true); // Scale x
            view.setFloat32(52, 0.19999998807907104, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, -3997, true);
            break;
        case NpcType.UlGibbon:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -27881, true);
            view.setFloat32(48, 1, true); // Jump appear
            view.setInt16(68, -7552, true);
            view.setInt16(70, 20554, true);
            break;
        case NpcType.ZolGibbon:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 6331, true);
            view.setInt16(68, -26688, true);
            view.setInt16(70, 565, true);
            break;
        case NpcType.Gibbles:
            view.setInt16(4, -1, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, -24296, true);
            view.setFloat32(44, 500, true); // Scale x
            view.setFloat32(52, 0.7999999523162842, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, -12210, true);
            break;
        case NpcType.Gee:
            view.setInt16(4, -1, true);
            view.setInt16(8, 6, true);
            view.setInt16(10, -20457, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, -4024, true);
            break;
        case NpcType.GiGue:
            view.setInt16(4, 32010, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 13852, true);
            view.setFloat32(44, 501, true); // Scale x
            view.setFloat32(52, 50, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 12374, true);
            break;
        case NpcType.IllGill:
            view.setInt16(4, 4104, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 7192, true);
            view.setInt16(68, -7552, true);
            view.setInt16(70, 24639, true);
            break;
        case NpcType.DelLily:
            view.setInt16(4, -1, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, 6388, true);
            view.setInt16(68, -26576, true);
            view.setInt16(70, 564, true);
            break;
        case NpcType.Epsilon:
            view.setInt16(4, -1, true);
            view.setInt16(8, 17, true);
            view.setInt16(10, -7914, true);
            view.setInt16(68, -4224, true);
            view.setInt16(70, -16379, true);
            break;
        case NpcType.GalGryphon:
            view.setInt16(4, 1173, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, -1, true);
            view.setInt16(68, 24624, true);
            view.setInt16(70, -29446, true);
            break;
        case NpcType.Deldepth:
            view.setInt16(4, 2095, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 6251, true);
            view.setInt16(68, -26352, true);
            view.setInt16(70, 665, true);
            break;
        case NpcType.Delbiter:
            view.setInt16(4, -1, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, -27880, true);
            view.setFloat32(48, 0.19999998807907104, true); // Confuse percent
            view.setFloat32(52, 20, true); // Confuse distance
            view.setFloat32(60, 0.5, true); // Charge percent
            view.setInt32(64, 1, true); // Type
            view.setInt16(68, -7552, true);
            view.setInt16(70, 24639, true);
            break;
        case NpcType.Dolmolm:
            view.setInt16(4, -1, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 28441, true);
            view.setFloat32(52, 1, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, 12370, true);
            break;
        case NpcType.Dolmdarl:
            view.setInt16(4, -1, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 31513, true);
            view.setFloat32(52, -1, true); // Scale z
            view.setInt16(68, -7552, true);
            view.setInt16(70, -4001, true);
            break;
        case NpcType.Morfos:
            view.setInt16(4, 1993, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 6115, true);
            view.setInt16(68, -25424, true);
            view.setInt16(70, 561, true);
            break;
        case NpcType.Recobox:
            view.setInt16(4, -1, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 6107, true);
            view.setInt16(68, -26160, true);
            view.setInt16(70, 686, true);
            break;
        case NpcType.SinowZoa:
            view.setInt16(4, 2634, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 6999, true);
            view.setFloat32(44, 1, true); // Scale x
            view.setInt16(68, -19488, true);
            view.setInt16(70, 665, true);
            break;
        case NpcType.SinowZele:
            view.setInt16(4, 2634, true);
            view.setInt16(8, 11, true);
            view.setInt16(10, 7027, true);
            view.setInt16(68, -25152, true);
            view.setInt16(70, 665, true);
            break;
        case NpcType.OlgaFlow:
            view.setInt16(4, -1, true);
            view.setInt16(8, 13, true);
            view.setInt16(10, 8466, true);
            view.setInt16(68, -4480, true);
            view.setInt16(70, -28572, true);
            break;
        case NpcType.SandRappy:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5471, true);
            view.setInt16(68, -27344, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.DelRappy:
            view.setInt16(4, -1, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, 5039, true);
            view.setInt16(68, -17168, true);
            view.setInt16(70, 410, true);
            break;
        case NpcType.Astark:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5653, true);
            view.setInt16(68, -26896, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.SatelliteLizard:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5524, true);
            view.setInt16(68, -27088, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.Yowie:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5547, true);
            view.setFloat32(44, 1, true); // Scale x
            view.setInt16(68, -25872, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.MerissaA:
            view.setInt16(4, -1, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 5322, true);
            view.setInt16(68, -16512, true);
            view.setInt16(70, 542, true);
            break;
        case NpcType.MerissaAA:
            view.setInt16(4, -1, true);
            view.setInt16(8, 8, true);
            view.setInt16(10, 5651, true);
            view.setInt16(68, -27328, true);
            view.setInt16(70, 1230, true);
            break;
        case NpcType.Girtablulu:
            view.setInt16(4, -1, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 5007, true);
            view.setInt16(68, -26256, true);
            view.setInt16(70, 459, true);
            break;
        case NpcType.Zu:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5734, true);
            view.setInt16(68, -28304, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.Pazuzu:
            view.setInt16(4, 937, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, 5054, true);
            view.setInt16(68, -15216, true);
            view.setInt16(70, 410, true);
            break;
        case NpcType.Boota:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5496, true);
            view.setInt16(68, -27216, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.ZeBoota:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5504, true);
            view.setInt16(68, -20304, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.BaBoota:
            view.setInt16(4, -1, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 5513, true);
            view.setInt16(68, -14800, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.Dorphon:
            view.setInt16(4, 2308, true);
            view.setInt16(8, 5, true);
            view.setInt16(10, 6840, true);
            view.setInt16(68, -26480, true);
            view.setInt16(70, 616, true);
            break;
        case NpcType.DorphonEclair:
            view.setInt16(4, 951, true);
            view.setInt16(8, 3, true);
            view.setInt16(10, 5101, true);
            view.setInt16(68, -30064, true);
            view.setInt16(70, 410, true);
            break;
        case NpcType.Goran:
            view.setInt16(4, -1, true);
            view.setInt16(8, 8, true);
            view.setInt16(10, 5439, true);
            view.setInt16(68, -27216, true);
            view.setInt16(70, 610, true);
            break;
        case NpcType.PyroGoran:
            view.setInt16(4, -1, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 5375, true);
            view.setInt16(68, -16384, true);
            view.setInt16(70, 542, true);
            break;
        case NpcType.GoranDetonator:
            view.setInt16(4, -1, true);
            view.setInt16(8, 7, true);
            view.setInt16(10, 5373, true);
            view.setInt16(68, -16384, true);
            view.setInt16(70, 542, true);
            break;
        case NpcType.SaintMilion:
            view.setInt16(4, 1297, true);
            view.setInt16(6, 24, true); // Clone count
            view.setInt16(8, 9, true);
            view.setInt16(10, 5521, true);
            view.setInt16(68, 28144, true);
            view.setInt16(70, 673, true);
            break;
        case NpcType.Shambertin:
            view.setInt16(4, 1362, true);
            view.setInt16(6, 24, true); // Clone count
            view.setInt16(8, 9, true);
            view.setInt16(10, 5662, true);
            view.setFloat32(44, 1, true); // Scale x
            view.setInt16(68, 31280, true);
            view.setInt16(70, 491, true);
            break;
    }
}
