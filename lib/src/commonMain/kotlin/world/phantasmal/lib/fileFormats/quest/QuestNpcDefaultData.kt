package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer

// TODO: set properties of friendly NPCs based on episode.
internal fun setNpcDefaultData(type: NpcType, view: Buffer) {
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (type) {
        NpcType.FemaleFat -> {
            view.setShort(2, 2)
            view.setShort(4, 1872)
            view.setShort(10, 5969)
            view.setShort(68, -16432)
            view.setShort(70, 1834)
        }
        NpcType.FemaleMacho -> {
            view.setShort(2, 2)
            view.setShort(4, 1876)
            view.setShort(10, 5973)
            view.setShort(68, -12528)
            view.setShort(70, 1834)
        }
        NpcType.FemaleTall -> {
            view.setShort(2, 2)
            view.setShort(4, 1883)
            view.setShort(68, -5504)
            view.setShort(70, 1834)
        }
        NpcType.MaleDwarf -> {
            view.setShort(2, 2)
            view.setShort(4, 1873)
            view.setShort(10, 5970)
            view.setShort(68, -15456)
            view.setShort(70, 1834)
        }
        NpcType.MaleFat -> {
            view.setShort(2, 2)
            view.setShort(4, 1882)
            view.setShort(10, 5979)
            view.setShort(68, -6528)
            view.setShort(70, 1834)
        }
        NpcType.MaleMacho -> {
            view.setShort(2, 2)
            view.setShort(4, 1880)
            view.setShort(10, 5977)
            view.setShort(68, -8576)
            view.setShort(70, 1834)
        }
        NpcType.MaleOld -> {
            view.setShort(2, 2)
            view.setShort(4, 1878)
            view.setShort(10, 5975)
            view.setShort(68, -10576)
            view.setShort(70, 1834)
        }
        NpcType.BlueSoldier -> {
            view.setShort(2, 2)
            view.setShort(4, 1875)
            view.setShort(10, 5972)
            view.setShort(68, -13504)
            view.setShort(70, 1834)
        }
        NpcType.RedSoldier -> {
            view.setShort(2, 2)
            view.setShort(4, 1874)
            view.setShort(10, 5971)
            view.setShort(68, -14480)
            view.setShort(70, 1834)
        }
        NpcType.Principal -> {
            view.setShort(4, 1888)
            view.setShort(10, 5985)
            view.setShort(68, -384)
            view.setShort(70, 1834)
        }
        NpcType.Tekker -> {
            view.setShort(2, 2)
            view.setShort(4, 1879)
            view.setShort(10, 5976)
            view.setShort(68, -9600)
            view.setShort(70, 1834)
        }
        NpcType.GuildLady -> {
            view.setShort(2, 2)
            view.setShort(4, 1891)
            view.setShort(10, 5988)
            view.setShort(68, 11584)
            view.setShort(70, 1835)
        }
        NpcType.Scientist -> {
            view.setShort(2, 2)
            view.setShort(4, 1877)
            view.setShort(10, 5974)
            view.setShort(68, -11552)
            view.setShort(70, 1834)
        }
        NpcType.Nurse -> {
            view.setShort(2, 2)
            view.setShort(4, 1884)
            view.setShort(10, 5981)
            view.setShort(68, -4480)
            view.setShort(70, 1834)
        }
        NpcType.Irene -> {
            view.setShort(4, 1889)
            view.setShort(10, 5986)
            view.setShort(68, 640)
            view.setShort(70, 1835)
        }
        NpcType.ItemShop -> {
            view.setShort(4, 8)
            view.setShort(10, 6453)
            view.setShort(68, 16560)
            view.setShort(70, 1176)
        }
        NpcType.Nurse2 -> {
            view.setShort(4, 2330)
            view.setShort(10, 6496)
            view.setShort(68, -13280)
            view.setShort(70, 1200)
        }
        NpcType.Hildebear -> {
            view.setShort(4, -1)
            view.setShort(8, 2)
            view.setShort(10, -1)
            view.setFloat(44, 1.000000238418579f) // Scale x
            view.setShort(68, 29968)
            view.setShort(70, -29446)
        }
        NpcType.RagRappy -> {
            view.setShort(8, 1)
            view.setShort(10, -1)
            view.setShort(68, 1072)
            view.setShort(70, -29444)
        }
        NpcType.Monest -> {
            view.setFloat(48, 5.000000953674316f) // Start number
            view.setFloat(52, 10.000004768371582f) // Total number
        }
        NpcType.BarbarousWolf -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 6475)
            view.setShort(68, 8576)
            view.setShort(70, -29445)
        }
        NpcType.Booma -> {
            view.setFloat(44, 0.30000001192092896f) // Scale x
            view.setFloat(48, 40.00001907348633f) // Idle distance
        }
        NpcType.Gobooma -> {
            view.setShort(8, 1)
            view.setShort(10, -1)
            view.setFloat(44, 0.30000001192092896f) // Scale x
            view.setFloat(48, 40.00001907348633f) // Idle distance
            view.setShort(68, 11600)
            view.setShort(70, -29444)
        }
        NpcType.Gigobooma -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 6492)
            view.setShort(68, -4000)
            view.setShort(70, -29446)
        }
        NpcType.Dragon -> {
            view.setShort(4, 1173)
            view.setShort(8, 11)
            view.setShort(10, -1)
            view.setShort(68, 24624)
            view.setShort(70, -29446)
        }
        NpcType.GrassAssassin -> view.setShort(8, 4)
        NpcType.PoisonLily -> view.setShort(8, 4)
        NpcType.NanoDragon -> view.setShort(8, 3)
        NpcType.EvilShark -> view.setShort(8, 3)
        NpcType.PalShark -> view.setShort(8, 3)
        NpcType.GuilShark -> view.setShort(8, 4)
        NpcType.PofuillySlime -> view.setShort(8, 4)
        NpcType.PanArms -> view.setShort(8, 5)
        NpcType.DeRolLe -> {
            view.setShort(4, 1485)
            view.setShort(6, 19) // Clone count
            view.setShort(8, 12)
            view.setShort(10, -1)
            view.setShort(68, -11088)
            view.setShort(70, -29445)
        }
        NpcType.Dubchic -> {
            view.setShort(4, 2626)
            view.setShort(8, 7)
            view.setShort(10, 7272)
            view.setShort(68, -25504)
            view.setShort(70, 561)
        }
        NpcType.Gilchic -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -1)
            view.setFloat(44, 1.000000238418579f) // Scale x
            view.setShort(68, 5968)
            view.setShort(70, -29444)
        }
        NpcType.Garanz -> {
            view.setShort(4, -1)
            view.setShort(8, 7)
            view.setShort(10, 6319)
            view.setShort(68, -26128)
            view.setShort(70, 561)
        }
        NpcType.SinowBeat -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -1)
            view.setShort(68, 6288)
            view.setShort(70, -29444)
        }
        NpcType.SinowGold -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -1)
            view.setFloat(44, -1.000000238418579f) // Scale x
            view.setShort(68, 8048)
            view.setShort(70, -29444)
        }
        NpcType.Canadine -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -1)
            view.setShort(68, 8496)
            view.setShort(70, -29444)
        }
        NpcType.Canane -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -1)
            view.setShort(68, 7264)
            view.setShort(70, -29444)
        }
        NpcType.Dubswitch -> {
            view.setShort(4, 2626)
            view.setShort(8, 7)
            view.setShort(10, 7298)
            view.setShort(68, -16736)
            view.setShort(70, 561)
        }
        NpcType.VolOptPart1 -> view.setShort(6, 35) // Clone count
        NpcType.VolOptPart2 -> view.setShort(8, 13)
        NpcType.DarkFalz -> {
            view.setShort(4, -1)
            view.setShort(8, 14)
            view.setShort(10, 7458)
            view.setShort(68, 25008)
            view.setShort(70, -29446)
        }
        NpcType.Hildebear2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 30745)
            view.setFloat(44, 1f) // Scale x
            view.setShort(68, -7296)
            view.setShort(70, -32759)
        }
        NpcType.RagRappy2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, -7401)
            view.setShort(68, -7296)
            view.setShort(70, 8201)
        }
        NpcType.Monest2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 4122)
            view.setFloat(48, 3f) // Start number
            view.setFloat(52, 9f) // Total number
            view.setShort(68, -7296)
            view.setShort(70, -12252)
        }
        NpcType.PoisonLily2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 26648)
            view.setShort(68, -7296)
            view.setShort(70, 8230)
        }
        NpcType.GrassAssassin2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 20761)
            view.setShort(68, -7296)
            view.setShort(70, 24595)
        }
        NpcType.Dimenian2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 28696)
            view.setShort(68, -7296)
            view.setShort(70, -4086)
        }
        NpcType.LaDimenian2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, 7449)
            view.setShort(68, -7296)
            view.setShort(70, -16367)
        }
        NpcType.SoDimenian2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, -1254)
            view.setFloat(48, 100f) // Idle distance
            view.setShort(68, -7040)
            view.setShort(70, 8372)
        }
        NpcType.DarkBelra2 -> {
            view.setShort(4, -1)
            view.setShort(8, 1)
            view.setShort(10, -17895)
            view.setShort(68, -7040)
            view.setShort(70, -32642)
        }
        NpcType.BarbaRay -> {
            view.setShort(4, -1)
            view.setShort(8, 14)
            view.setShort(10, 23572)
            view.setShort(68, -2688)
            view.setShort(70, 24576)
        }
        NpcType.SavageWolf2 -> {
            view.setShort(4, 11785)
            view.setShort(8, 3)
            view.setShort(10, -20711)
            view.setShort(68, -7552)
            view.setShort(70, 8250)
        }
        NpcType.BarbarousWolf2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, -14056)
            view.setShort(68, -7552)
            view.setShort(70, -32650)
        }
        NpcType.PanArms2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, -6632)
            view.setShort(68, -7552)
            view.setShort(70, -16251)
        }
        NpcType.Dubchic2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, -3560)
            view.setShort(68, -7552)
            view.setShort(70, 16513)
        }
        NpcType.Gilchic2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, -23272)
            view.setShort(68, -7552)
            view.setShort(70, -32654)
        }
        NpcType.Garanz2 -> {
            view.setShort(4, 27144)
            view.setShort(8, 3)
            view.setShort(10, 27928)
            view.setShort(68, -7552)
            view.setShort(70, 24683)
        }
        NpcType.Dubswitch2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, -4840)
            view.setShort(68, -7552)
            view.setShort(70, -20363)
        }
        NpcType.Delsaber2 -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, 2841)
            view.setShort(68, -7552)
            view.setShort(70, 16513)
        }
        NpcType.ChaosSorcerer2 -> {
            view.setShort(4, -1)
            view.setShort(8, 4)
            view.setShort(10, 9754)
            view.setShort(68, -7296)
            view.setShort(70, -7963)
        }
        NpcType.GolDragon -> {
            view.setShort(4, -19963)
            view.setShort(8, 15)
            view.setShort(10, -18411)
            view.setShort(68, -3712)
            view.setShort(70, 16555)
        }
        NpcType.SinowBerill -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 15896)
            view.setFloat(44, 3f) // Scale x
            view.setFloat(52, -0.19999998807907104f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 47)
        }
        NpcType.SinowSpigell -> {
            view.setShort(4, 880)
            view.setShort(8, 5)
            view.setShort(10, 7101)
            view.setShort(68, -11584)
            view.setShort(70, 1163)
        }
        NpcType.Merillia -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, 28439)
            view.setFloat(44, 1f) // Scale x
            view.setFloat(52, -0.09999999403953552f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 16456)
        }
        NpcType.Meriltas -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, 30999)
            view.setFloat(44, -1f) // Scale x
            view.setFloat(52, 0.09999999403953552f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 16456)
        }
        NpcType.Mericarol -> {
            view.setShort(4, -1)
            view.setShort(8, 17)
            view.setShort(10, 30232)
            view.setFloat(44, 0.19999998807907104f) // Scale x
            view.setFloat(52, 0.19999998807907104f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, -4016)
        }
        NpcType.Mericus -> {
            view.setShort(4, 32010)
            view.setShort(8, 17)
            view.setShort(10, 3356)
            view.setFloat(44, 0.19999998807907104f) // Scale x
            view.setFloat(52, 0.19999998807907104f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 28762)
        }
        NpcType.Merikle -> {
            view.setShort(4, 32010)
            view.setShort(8, 17)
            view.setShort(10, 3868)
            view.setFloat(44, 0.19999998807907104f) // Scale x
            view.setFloat(52, 0.19999998807907104f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, -3997)
        }
        NpcType.UlGibbon -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -27881)
            view.setFloat(48, 1f) // Jump appear
            view.setShort(68, -7552)
            view.setShort(70, 20554)
        }
        NpcType.ZolGibbon -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 6331)
            view.setShort(68, -26688)
            view.setShort(70, 565)
        }
        NpcType.Gibbles -> {
            view.setShort(4, -1)
            view.setShort(8, 17)
            view.setShort(10, -24296)
            view.setFloat(44, 500f) // Scale x
            view.setFloat(52, 0.7999999523162842f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, -12210)
        }
        NpcType.Gee -> {
            view.setShort(4, -1)
            view.setShort(8, 6)
            view.setShort(10, -20457)
            view.setShort(68, -7552)
            view.setShort(70, -4024)
        }
        NpcType.GiGue -> {
            view.setShort(4, 32010)
            view.setShort(8, 17)
            view.setShort(10, 13852)
            view.setFloat(44, 501f) // Scale x
            view.setFloat(52, 50f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 12374)
        }
        NpcType.IllGill -> {
            view.setShort(4, 4104)
            view.setShort(8, 17)
            view.setShort(10, 7192)
            view.setShort(68, -7552)
            view.setShort(70, 24639)
        }
        NpcType.DelLily -> {
            view.setShort(4, -1)
            view.setShort(8, 17)
            view.setShort(10, 6388)
            view.setShort(68, -26576)
            view.setShort(70, 564)
        }
        NpcType.Epsilon -> {
            view.setShort(4, -1)
            view.setShort(8, 17)
            view.setShort(10, -7914)
            view.setShort(68, -4224)
            view.setShort(70, -16379)
        }
        NpcType.GalGryphon -> {
            view.setShort(4, 1173)
            view.setShort(8, 11)
            view.setShort(10, -1)
            view.setShort(68, 24624)
            view.setShort(70, -29446)
        }
        NpcType.Deldepth -> {
            view.setShort(4, 2095)
            view.setShort(8, 11)
            view.setShort(10, 6251)
            view.setShort(68, -26352)
            view.setShort(70, 665)
        }
        NpcType.Delbiter -> {
            view.setShort(4, -1)
            view.setShort(8, 11)
            view.setShort(10, -27880)
            view.setFloat(48, 0.19999998807907104f) // Confuse percent
            view.setFloat(52, 20f) // Confuse distance
            view.setFloat(60, 0.5f) // Charge percent
            view.setInt(64, 1) // Type
            view.setShort(68, -7552)
            view.setShort(70, 24639)
        }
        NpcType.Dolmolm -> {
            view.setShort(4, -1)
            view.setShort(8, 11)
            view.setShort(10, 28441)
            view.setFloat(52, 1f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, 12370)
        }
        NpcType.Dolmdarl -> {
            view.setShort(4, -1)
            view.setShort(8, 11)
            view.setShort(10, 31513)
            view.setFloat(52, -1f) // Scale z
            view.setShort(68, -7552)
            view.setShort(70, -4001)
        }
        NpcType.Morfos -> {
            view.setShort(4, 1993)
            view.setShort(8, 11)
            view.setShort(10, 6115)
            view.setShort(68, -25424)
            view.setShort(70, 561)
        }
        NpcType.Recobox -> {
            view.setShort(4, -1)
            view.setShort(8, 11)
            view.setShort(10, 6107)
            view.setShort(68, -26160)
            view.setShort(70, 686)
        }
        NpcType.SinowZoa -> {
            view.setShort(4, 2634)
            view.setShort(8, 11)
            view.setShort(10, 6999)
            view.setFloat(44, 1f) // Scale x
            view.setShort(68, -19488)
            view.setShort(70, 665)
        }
        NpcType.SinowZele -> {
            view.setShort(4, 2634)
            view.setShort(8, 11)
            view.setShort(10, 7027)
            view.setShort(68, -25152)
            view.setShort(70, 665)
        }
        NpcType.OlgaFlow -> {
            view.setShort(4, -1)
            view.setShort(8, 13)
            view.setShort(10, 8466)
            view.setShort(68, -4480)
            view.setShort(70, -28572)
        }
        NpcType.SandRappy -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5471)
            view.setShort(68, -27344)
            view.setShort(70, 616)
        }
        NpcType.DelRappy -> {
            view.setShort(4, -1)
            view.setShort(8, 3)
            view.setShort(10, 5039)
            view.setShort(68, -17168)
            view.setShort(70, 410)
        }
        NpcType.Astark -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5653)
            view.setShort(68, -26896)
            view.setShort(70, 616)
        }
        NpcType.SatelliteLizard -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5524)
            view.setShort(68, -27088)
            view.setShort(70, 616)
        }
        NpcType.Yowie -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5547)
            view.setFloat(44, 1f) // Scale x
            view.setShort(68, -25872)
            view.setShort(70, 616)
        }
        NpcType.MerissaA -> {
            view.setShort(4, -1)
            view.setShort(8, 7)
            view.setShort(10, 5322)
            view.setShort(68, -16512)
            view.setShort(70, 542)
        }
        NpcType.MerissaAA -> {
            view.setShort(4, -1)
            view.setShort(8, 8)
            view.setShort(10, 5651)
            view.setShort(68, -27328)
            view.setShort(70, 1230)
        }
        NpcType.Girtablulu -> {
            view.setShort(4, -1)
            view.setShort(8, 7)
            view.setShort(10, 5007)
            view.setShort(68, -26256)
            view.setShort(70, 459)
        }
        NpcType.Zu -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5734)
            view.setShort(68, -28304)
            view.setShort(70, 616)
        }
        NpcType.Pazuzu -> {
            view.setShort(4, 937)
            view.setShort(8, 3)
            view.setShort(10, 5054)
            view.setShort(68, -15216)
            view.setShort(70, 410)
        }
        NpcType.Boota -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5496)
            view.setShort(68, -27216)
            view.setShort(70, 616)
        }
        NpcType.ZeBoota -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5504)
            view.setShort(68, -20304)
            view.setShort(70, 616)
        }
        NpcType.BaBoota -> {
            view.setShort(4, -1)
            view.setShort(8, 5)
            view.setShort(10, 5513)
            view.setShort(68, -14800)
            view.setShort(70, 616)
        }
        NpcType.Dorphon -> {
            view.setShort(4, 2308)
            view.setShort(8, 5)
            view.setShort(10, 6840)
            view.setShort(68, -26480)
            view.setShort(70, 616)
        }
        NpcType.DorphonEclair -> {
            view.setShort(4, 951)
            view.setShort(8, 3)
            view.setShort(10, 5101)
            view.setShort(68, -30064)
            view.setShort(70, 410)
        }
        NpcType.Goran -> {
            view.setShort(4, -1)
            view.setShort(8, 8)
            view.setShort(10, 5439)
            view.setShort(68, -27216)
            view.setShort(70, 610)
        }
        NpcType.PyroGoran -> {
            view.setShort(4, -1)
            view.setShort(8, 7)
            view.setShort(10, 5375)
            view.setShort(68, -16384)
            view.setShort(70, 542)
        }
        NpcType.GoranDetonator -> {
            view.setShort(4, -1)
            view.setShort(8, 7)
            view.setShort(10, 5373)
            view.setShort(68, -16384)
            view.setShort(70, 542)
        }
        NpcType.SaintMilion -> {
            view.setShort(4, 1297)
            view.setShort(6, 24) // Clone count
            view.setShort(8, 9)
            view.setShort(10, 5521)
            view.setShort(68, 28144)
            view.setShort(70, 673)
        }
        NpcType.Shambertin -> {
            view.setShort(4, 1362)
            view.setShort(6, 24) // Clone count
            view.setShort(8, 9)
            view.setShort(10, 5662)
            view.setFloat(44, 1f) // Scale x
            view.setShort(68, 31280)
            view.setShort(70, 491)
        }
    }
}
