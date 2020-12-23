package world.phantasmal.lib.test

/**
 * Applies [process] to all quest files provided with Tethealla version 0.143.
 * [process] is called with the path to the file and the file name.
 */
inline fun testWithTetheallaQuests(process: (path: String, filename: String) -> Unit) {
    for (file in TETHEALLA_QUESTS) {
        val lastSlashIdx = file.lastIndexOf('/')
        process(TETHEALLA_QUEST_PATH_PREFIX + file, file.drop(lastSlashIdx + 1))
    }
}

const val TETHEALLA_QUEST_PATH_PREFIX = "/tethealla_v0.143_quests"

val TETHEALLA_QUESTS = listOf(
    "/battle/1.qst",
    "/battle/2.qst",
    "/battle/3.qst",
    "/battle/4.qst",
    "/battle/5.qst",
    "/battle/6.qst",
    "/battle/7.qst",
    "/battle/8.qst",
    "/chl/ep1/1.qst",
    "/chl/ep1/2.qst",
    "/chl/ep1/3.qst",
    "/chl/ep1/4.qst",
    "/chl/ep1/5.qst",
    "/chl/ep1/6.qst",
    "/chl/ep1/7.qst",
    "/chl/ep1/8.qst",
    "/chl/ep1/9.qst",
    "/chl/ep2/21.qst",
    "/chl/ep2/22.qst",
    "/chl/ep2/23.qst",
    "/chl/ep2/24.qst",
    "/chl/ep2/25.qst",
    "/ep1/event/ma1.qst",
    "/ep1/event/ma4-a.qst",
    "/ep1/event/ma4-b.qst",
    "/ep1/event/ma4-c.qst",
    "/ep1/event/princgift.qst",
    "/ep1/event/sunset base.qst",
    "/ep1/event/whiteday.qst",
    "/ep1/ext/en1.qst",
    "/ep1/ext/en2.qst",
    "/ep1/ext/en3.qst",
    "/ep1/ext/en4.qst",
    "/ep1/ext/mop-up1.qst",
    "/ep1/ext/mop-up2.qst",
    "/ep1/ext/mop-up3.qst",
    "/ep1/ext/mop-up4.qst",
    "/ep1/ext/todays rate.qst",
    "/ep1/recovery/fragmentofmemoryen.qst",
    "/ep1/recovery/gallon.qst",
    "/ep1/recovery/lost havoc vulcan.qst",
    "/ep1/recovery/lost heat sword.qst",
    "/ep1/recovery/lost ice spinner.qst",
    "/ep1/recovery/lost soul blade.qst",
    "/ep1/recovery/rappy holiday.qst",
    "/ep1/vr/labyrinthe trial.qst",
    "/ep1/vr/ttf.qst",
    "/ep2/event/beach laughter.qst",
    "/ep2/event/christmas.qst",
    "/ep2/event/dream messenger.qst",
    "/ep2/event/halloween.qst",
    "/ep2/event/ma2.qst",
    // ma4-a.qst seems corrupt, doesn't work in qedit either.
//    "/ep2/event/ma4-a.qst",
    "/ep2/event/ma4-b.qst",
    "/ep2/event/ma4-c.qst",
    "/ep2/event/quest239.qst",
    "/ep2/event/singing by the beach.qst",
    "/ep2/ext/pw1.qst",
    "/ep2/ext/pw2.qst",
    "/ep2/ext/pw3.qst",
    "/ep2/ext/pw4.qst",
    "/ep2/shop/gallon.qst",
    "/ep2/tower/east.qst",
    "/ep2/tower/west.qst",
    "/ep2/vr/reach for the dream.qst",
    "/ep2/vr/respectivetomorrow.qst",
    "/ep4/event/clarie's deal.qst",
    "/ep4/event/login.qst",
    "/ep4/event/ma4-a.qst",
    "/ep4/event/ma4-b.qst",
    "/ep4/event/ma4-c.qst",
    "/ep4/event/wildhouse.qst",
    "/ep4/ext/newwipe1.qst",
    "/ep4/ext/newwipe2.qst",
    "/ep4/ext/newwipe3.qst",
    "/ep4/ext/newwipe4.qst",
    "/ep4/ext/newwipe5.qst",
    "/ep4/ext/waroflimit1.qst",
    "/ep4/ext/waroflimit2.qst",
    "/ep4/ext/waroflimit3.qst",
    "/ep4/ext/waroflimit4.qst",
    "/ep4/ext/waroflimit5.qst",
    "/ep4/shop/itempresent.qst",
    "/ep4/shop/quest205.qst",
    "/ep4/vr/max3.qst",
    "/princ/ep1/1-1.qst",
    "/princ/ep1/1-2.qst",
    "/princ/ep1/1-3.qst",
    "/princ/ep1/2-1.qst",
    "/princ/ep1/2-2.qst",
    "/princ/ep1/2-3.qst",
    "/princ/ep1/2-4.qst",
    "/princ/ep1/3-1.qst",
    "/princ/ep1/3-2.qst",
    "/princ/ep1/3-3.qst",
    "/princ/ep1/4-1.qst",
    "/princ/ep1/4-2.qst",
    "/princ/ep1/4-3.qst",
    "/princ/ep1/4-4.qst",
    "/princ/ep1/4-5.qst",
    "/princ/ep2/quest451.raw",
    "/princ/ep2/quest452.raw",
    "/princ/ep2/quest453.raw",
    "/princ/ep2/quest454.raw",
    "/princ/ep2/quest455.raw",
    "/princ/ep2/quest456.raw",
    "/princ/ep2/quest457.raw",
    "/princ/ep2/quest458.raw",
    "/princ/ep2/quest459.raw",
    "/princ/ep2/quest460.raw",
    "/princ/ep2/quest461.raw",
    "/princ/ep2/quest462.raw",
    "/princ/ep2/quest463.raw",
    "/princ/ep2/quest464.raw",
    "/princ/ep2/quest465.raw",
    "/princ/ep2/quest466.raw",
    "/princ/ep2/quest467.raw",
    "/princ/ep2/quest468.raw",
    "/princ/ep4/9-1.qst",
    "/princ/ep4/9-2.qst",
    "/princ/ep4/9-3.qst",
    "/princ/ep4/9-4.qst",
    "/princ/ep4/9-5.qst",
    "/princ/ep4/9-6.qst",
    "/princ/ep4/9-7.qst",
    "/princ/ep4/9-8.qst",
    "/princ/ep4/pod.qst",
    "/solo/ep1/01.qst",
    "/solo/ep1/02.qst",
    "/solo/ep1/03.qst",
    "/solo/ep1/04.qst",
    "/solo/ep1/05.qst",
    "/solo/ep1/06.qst",
    "/solo/ep1/07.qst",
    "/solo/ep1/08.qst",
    "/solo/ep1/09.qst",
    "/solo/ep1/10.qst",
    "/solo/ep1/11.qst",
    "/solo/ep1/12.qst",
    "/solo/ep1/13.qst",
    "/solo/ep1/14.qst",
    "/solo/ep1/15.qst",
    "/solo/ep1/16.qst",
    "/solo/ep1/17.qst",
    "/solo/ep1/18.qst",
    "/solo/ep1/19.qst",
    "/solo/ep1/20.qst",
    "/solo/ep1/21.qst",
    "/solo/ep1/22.qst",
    "/solo/ep1/23.qst",
    "/solo/ep1/24.qst",
    "/solo/ep1/25.qst",
    "/solo/ep1/side/26.qst",
    "/solo/ep1/side/goodluck.qst",
    "/solo/ep1/side/quest035.qst",
    "/solo/ep1/side/quest073.qst",
    "/solo/ep2/01.qst",
    "/solo/ep4/01-blackpaper.qst",
    "/solo/ep4/02-pioneer spirit.qst",
    "/solo/ep4/03-Warrior Pride.qst",
    "/solo/ep4/04-Restless Lion.qst",
    "/solo/ep4/blackpaper2.qst",
    "/solo/ep4/wilderending.qst",
)
