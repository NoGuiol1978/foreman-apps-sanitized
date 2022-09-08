package mn.foreman.antminer;

import mn.foreman.model.MinerType;

import com.google.common.collect.ImmutableMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** All of the known Antminer types. */
public enum AntminerType
        implements MinerType {

    /** Antminer A3. */
    ANTMINER_A3(
            "Antminer A3",
            "antminer-a3"),

    /** Antminer B3. */
    ANTMINER_B3(
            "Antminer B3",
            "antminer-b3"),

    /** Antminer B7. */
    ANTMINER_B7(
            "Antminer B7",
            "antminer-b7"),

    /** Antminer D3. */
    ANTMINER_D3(
            "Antminer D3",
            "antminer-d3"),

    /** Antminer D7. */
    ANTMINER_D7(
            "Antminer D7",
            "antminer-d7"),

    /** Antminer DR3. */
    ANTMINER_DR3(
            "Antminer DR3",
            "antminer-dr3"),

    /** Antminer DR5. */
    ANTMINER_DR5(
            "Antminer DR5",
            "antminer-dr5"),

    /** Antminer E3. */
    ANTMINER_E3(
            "Antminer E3",
            "antminer-e3"),

    /** Antminer E7. */
    ANTMINER_E7(
            "Antminer E7",
            "antminer-e7"),

    /** Antminer L3. */
    ANTMINER_L3(
            "Antminer L3",
            "antminer-l3"),

    /** Antminer L3+. */
    ANTMINER_L3P(
            "Antminer L3+",
            "antminer-l3+"),

    /** Antminer L3+ vnish. */
    ANTMINER_L3P_VNISH(
            "Antminer L3+ (vnish",
            "antminer-l3+"),

    /** Antminer L3+ blissz. */
    ANTMINER_L3P_BLISSZ_1_02(
            "Antminer L3+ Blissz",
            "antminer-l3+"),

    /** Antminer L3+ hiveon. */
    ANTMINER_L3P_HIVEON(
            "Antminer L3+ Hiveon",
            "antminer-l3+"),

    /** Antminer L3++. */
    ANTMINER_L3PP(
            "Antminer L3++",
            "antminer-l3++"),

    /** Antminer L7. */
    ANTMINER_L7(
            "Antminer L7",
            "antminer-l7"),

    /** Antminer L7 (7800). */
    ANTMINER_L7_7800(
            "Antminer L7 (7800)",
            "antminer-l7-7800"),

    /** Antminer L7 (8050). */
    ANTMINER_L7_8050(
            "Antminer L7 (8050)",
            "antminer-l7-8050"),

    /** Antminer L7 (8300). */
    ANTMINER_L7_8300(
            "Antminer L7 (8300)",
            "antminer-l7-8300"),

    /** Antminer L7 (8550). */
    ANTMINER_L7_8550(
            "Antminer L7 (8550)",
            "antminer-l7-8550"),

    /** Antminer L7 (8800). */
    ANTMINER_L7_8800(
            "Antminer L7 (8800)",
            "antminer-l7-8800"),

    /** Antminer L7 (9050). */
    ANTMINER_L7_9050(
            "Antminer L7 (9050)",
            "antminer-l7-9050"),

    /** Antminer L7 (9300). */
    ANTMINER_L7_9300(
            "Antminer L7 (9300)",
            "antminer-l7-9300"),

    /** Antminer L7 (9500). */
    ANTMINER_L7_9500(
            "Antminer L7 (9500)",
            "antminer-l7-9500"),

    /** Antminer S7. */
    ANTMINER_S7(
            "Antminer S7",
            "antminer-s7"),

    /** Antminer S9. */
    ANTMINER_S9(
            "Antminer S9",
            "antminer-s9"),

    /** Antminer S9+. */
    ANTMINER_S9P(
            "Antminer S9+",
            "antminer-s9+"),

    /** Antminer S9D. */
    ANTMINER_S9D(
            "Antminer S9D",
            "antminer-s9d"),

    /** Antminer S9 A.P. */
    ANTMINER_S9_AP_3_8_6(
            "Antminer S9 (A.P. 3.8.6)",
            "antminer-s9"),

    /** Antminer S9 vnish */
    ANTMINER_S9_VNISH_3_6_8(
            "Antminer S9 (vnish 3.6.8)",
            "antminer-s9"),

    /** Antminer S9 vnish */
    ANTMINER_S9_VNISH_3_8_6(
            "Antminer S9 (vnish 3.8.6)",
            "antminer-s9"),

    /** Antminer S9 vnish */
    ANTMINER_S9_VNISH_AWESOME_3_8_6(
            "Antminer S9 (vnish for AWESOME 3.8.6)",
            "antminer-s9"),

    /** Antminer S9 Hiveon */
    ANTMINER_S9_HIVEON(
            "Antminer S9 Hiveon",
            "antminer-s9"),

    /** Antminer S9i. */
    ANTMINER_S9I(
            "Antminer S9i",
            "antminer-s9i"),

    /** Antminer S9j. */
    ANTMINER_S9J(
            "Antminer S9j",
            "antminer-s9j"),

    /** Antminer S9k. */
    ANTMINER_S9K(
            "Antminer S9k",
            "antminer-s9k"),

    /** Antminer S9 Hydro. */
    ANTMINER_S9_HYDRO(
            "Antminer S9 Hydro",
            "antminer-s9-hydro"),

    /** Antminer S11. */
    ANTMINER_S11(
            "Antminer S11",
            "antminer-s11"),

    /** Antminer S15. */
    ANTMINER_S15(
            "Antminer S15",
            "antminer-s15"),

    /** Antminer X17. */
    ANTMINER_X17(
            "Antminer X17",
            "antminer-s17"),

    /** Antminer S17. */
    ANTMINER_S17(
            "Antminer S17",
            "antminer-s17"),

    /** Antminer S17 vnish. */
    ANTMINER_S17_VNISH_1_1_0(
            "Antminer S17 (vnish 1.1.0)",
            "antminer-s17"),

    /** Antminer S17 vnish. */
    ANTMINER_S17_VNISH_2_0_1(
            "Antminer S17 (vnish 2.0.1)",
            "antminer-s17"),

    /** Antminer S17 vnish. */
    ANTMINER_S17_VNISH_2_0_3(
            "Antminer S17 (vnish 2.0.3)",
            "antminer-s17"),

    /** Antminer S17 vnish. */
    ANTMINER_S17_VNISH_AWESOME_2_0_3(
            "Antminer S17 (vnish for AWESOME 2.0.1)",
            "antminer-s17"),

    /** Antminer S17+. */
    ANTMINER_S17P(
            "Antminer S17+",
            "antminer-s17p"),

    /** Antminer S17+ vnish. */
    ANTMINER_S17P_VNISH_2_0_3(
            "Antminer S17+ (vnish 2.0.3)",
            "antminer-s17p"),

    /** Antminer S17+ vnish. */
    ANTMINER_S17P_VNISH_2_0_3_1(
            "Antminer S17+ (vnish 2.0.3.1)",
            "antminer-s17p"),

    /** Antminer S17e. */
    ANTMINER_S17E(
            "Antminer S17e",
            "antminer-s17e"),

    /** Antminer S17 Pro. */
    ANTMINER_S17_PRO(
            "Antminer S17 Pro",
            "antminer-s17-pro"),

    /** Antminer S19i. */
    ANTMINER_S19I(
            "Antminer S19i",
            "antminer-s19i"),

    /** Antminer S19a. */
    ANTMINER_S19A(
            "Antminer S19a",
            "antminer-s19a"),

    /** Antminer S19p. */
    ANTMINER_S19P(
            "Antminer S19+",
            "antminer-s19p"),

    /** Antminer S19a (92T). */
    ANTMINER_S19A_92T(
            "Antminer S19a (92T)",
            "antminer-s19a-92t"),

    /** Antminer S19a (96T). */
    ANTMINER_S19A_96T(
            "Antminer S19a (96T)",
            "antminer-s19a-96t"),

    /** Antminer S19a Pro. */
    ANTMINER_S19A_PRO(
            "Antminer S19a Pro",
            "antminer-s19a-pro"),

    /** Antminer S19a Pro (110T). */
    ANTMINER_S19A_PRO_110T(
            "Antminer S19a Pro (110T)",
            "antminer-s19a-pro-110t"),

    /** Antminer S19 XP. */
    ANTMINER_S19_XP(
            "Antminer S19 XP",
            "antminer-s19-xp"),

    /** Antminer T9. */
    ANTMINER_T9(
            "Antminer T9",
            "antminer-t9"),

    /** Antminer T9+. */
    ANTMINER_T9P(
            "Antminer T9+",
            "antminer-t9+"),

    /** Antminer T9+ (vnish). */
    ANTMINER_T9P_VNISH_3_8_6(
            "Antminer T9+ (vnish 3.8.6)",
            "antminer-t9+"),

    /** Antminer T15. */
    ANTMINER_T15(
            "Antminer T15",
            "antminer-t15"),

    /** Antminer T17. */
    ANTMINER_T17(
            "Antminer T17",
            "antminer-t17"),

    /** Antminer T17 vnish. */
    ANTMINER_T17_VNISH_2_0_1(
            "Antminer T17 (vnish 2.0.1)",
            "antminer-t17"),

    /** Antminer T17 vnish. */
    ANTMINER_T17_VNISH_2_0_3(
            "Antminer T17 (vnish 2.0.3)",
            "antminer-t17"),

    /** Antminer T17 vnish. */
    ANTMINER_T17_VNISH_AWESOME_2_0_1(
            "Antminer T17 (vnish for AWESOME 2.0.1)",
            "antminer-t17"),

    /** Antminer T17 vnish. */
    ANTMINER_T17_VNISH_AWESOME_2_0_3(
            "Antminer T17 (vnish for AWESOME 2.0.3)",
            "antminer-t17"),

    /** Antminer T17+. */
    ANTMINER_T17P(
            "Antminer T17+",
            "antminer-t17p"),

    /** Antminer T17+ vnish. */
    ANTMINER_T17P_VNISH_2_0_3(
            "Antminer T17+ (vnish 2.0.3)",
            "antminer-t17p"),

    /** Antminer T17e. */
    ANTMINER_T17E(
            "Antminer T17e",
            "antminer-t17e"),

    /** Antminer T19 Hydro. */
    ANTMINER_T19_HYDRO(
            "Antminer T19 Hydro",
            "antminer-t19-hydro"),

    /** Antminer T19 Hydro (119T). */
    ANTMINER_T19_HYDRO_119T(
            "Antminer T19 Hydro (119T)",
            "antminer-t19-hydro-119t"),

    /** Antminer T19 Hydro (125.5T). */
    ANTMINER_T19_HYDRO_1255T(
            "Antminer T19 Hydro (125.5T)",
            "antminer-t19-hydro-125.5t"),

    /** Antminer T19 Hydro (132T). */
    ANTMINER_T19_HYDRO_132T(
            "Antminer T19 Hydro (132T)",
            "antminer-t19-hydro-132t"),

    /** Antminer T19 Hydro (138.5T). */
    ANTMINER_T19_HYDRO_1385T(
            "Antminer T19 Hydro (138.5T)",
            "antminer-t19-hydro-138.5t"),

    /** Antminer T19. */
    ANTMINER_T19(
            "Antminer T19",
            "antminer-t19"),

    /** Antminer T19 (84T). */
    ANTMINER_T19_84T(
            "Antminer T19 (84T)",
            "antminer-t19-84t"),

    /** Antminer T19 (88T). */
    ANTMINER_T19_88T(
            "Antminer T19 (88T)",
            "antminer-t19-88t"),

    /** Antminer T19 (vnish). */
    ANTMINER_T19_VNISH(
            "Antminer T19 (Vnish ",
            "antminer-t19-vnish"),

    /** Antminer X3. */
    ANTMINER_X3(
            "Antminer X3",
            "antminer-x3"),

    /** Antminer Z9. */
    ANTMINER_Z9(
            "Antminer Z9",
            "antminer-z9"),

    /** Antminer Z9-Mini. */
    ANTMINER_Z9M(
            "Antminer Z9-Mini",
            "antminer-z9-mini"),

    /** Antminer Z11. */
    ANTMINER_Z11(
            "Antminer Z11",
            "antminer-z11"),

    /** BraiinsOS S9. */
    BRAIINS_S9(
            "braiins-am1-s9",
            "antminer-s9"),

    /** BraiinsOS S17. */
    BRAIINS_S17(
            "braiins-am2-s17",
            "antminer-s17"),

    /** BraiinsOS S17 Pro. */
    BRAIINS_S17_PRO(
            "braiins-s17-pro",
            "antminer-s17"),

    /** BraiinsOS S17+. */
    BRAIINS_S17P(
            "braiins-s17+",
            "antminer-s17p"),

    /** BraiinsOS X17. */
    BRAIINS_X17(
            "braiins-am2-x17",
            "antminer-s17"),

    /** Braiins T19. */
    BRAIINS_T19(
            "braiins-t19",
            "antminer-t19"),

    /** Braiins S19. */
    BRAIINS_S19(
            "braiins-s19",
            "antminer-s19"),

    /** Braiins S19 PRO. */
    BRAIINS_S19PRO(
            "braiins-s19pro",
            "antminer-s19-pro"),

    /** Braiins S19J PRO. */
    BRAIINS_S19JPRO(
            "braiins-s19jpro",
            "antminer-s19j-pro"),

    /** Minecenter S9. */
    MINECENTER_S9(
            "Minecenter S9",
            "antminer-s9"),

    /** Antminer S19 Hydro. */
    ANTMINER_S19_HYDRO(
            "Antminer S19 Hydro",
            "antminer-s19-hydro"),

    /** Antminer S19 Hydro (132T). */
    ANTMINER_S19_HYDRO_132T(
            "Antminer S19 Hydro (132T)",
            "antminer-s19-hydro-132t"),

    /** Antminer S19 Hydro (138.5T). */
    ANTMINER_S19_HYDRO_1385T(
            "Antminer S19 Hydro (138.5T)",
            "antminer-s19-hydro-138.5t"),

    /** Antminer S19 Hydro (145T). */
    ANTMINER_S19_HYDRO_145T(
            "Antminer S19 Hydro (145T)",
            "antminer-s19-hydro-145t"),

    /** Antminer S19 Hydro (151.5T). */
    ANTMINER_S19_HYDRO_1515T(
            "Antminer S19 Hydro (151.5T)",
            "antminer-s19-hydro-151.5t"),

    /** Antminer S19. */
    ANTMINER_S19(
            "Antminer S19",
            "antminer-s19"),

    /** Antminer S19 (82T). */
    ANTMINER_S19_82T(
            "Antminer S19 (82T)",
            "antminer-s19-82t"),

    /** Antminer S19 (86T). */
    ANTMINER_S19_86T(
            "Antminer S19 (86T)",
            "antminer-s19-86t"),

    /** Antminer S19 (90T). */
    ANTMINER_S19_90T(
            "Antminer S19 (90T)",
            "antminer-s19-90t"),

    /** Antminer S19 (92T). */
    ANTMINER_S19_92T(
            "Antminer S19 (92T)",
            "antminer-s19-92t"),

    /** Antminer S19 (95T). */
    ANTMINER_S19_95T(
            "Antminer S19 (95T)",
            "antminer-s19-95t"),

    /** Antminer S19 vnish. */
    ANTMINER_S19_VNISH_OLD(
            "Antminer S19 Vnish",
            "antminer-s19"),

    /** Antminer S19 Pro Hydro. */
    ANTMINER_S19_PRO_HYDRO(
            "Antminer S19Pro Hydro",
            "antminer-s19-pro-hydro"),

    /** Antminer S19 Pro Hydro (154.5T). */
    ANTMINER_S19_PRO_HYDRO_1545T(
            "Antminer S19Pro Hydro (154.5T)",
            "antminer-s19-pro-hydro-1545t"),

    /** Antminer S19 Pro Hydro (162T). */
    ANTMINER_S19_PRO_HYDRO_162T(
            "Antminer S19Pro Hydro (162T)",
            "antminer-s19-pro-hydro-162t"),

    /** Antminer S19 Pro Hydro (169.5T). */
    ANTMINER_S19_PRO_HYDRO_1695T(
            "Antminer S19Pro Hydro (169.5T)",
            "antminer-s19-pro-hydro-1695t"),

    /** Antminer S19 Pro+ Hydro. */
    ANTMINER_S19_PROP_HYDRO(
            "Antminer S19 Pro+ Hyd.",
            "antminer-s19-prop-hydro"),

    /** Antminer S19 Pro+ Hydro (170T). */
    ANTMINER_S19_PROP_HYDRO_170T(
            "Antminer S19 Pro+ Hyd. (170TH)",
            "antminer-s19-prop-hydro-170t"),

    /** Antminer S19 Pro+ Hydro (177T). */
    ANTMINER_S19_PROP_HYDRO_177T(
            "Antminer S19 Pro+ Hyd. (177TH)",
            "antminer-s19-prop-hydro-177t"),

    /** Antminer S19 Pro+ Hydro (184T). */
    ANTMINER_S19_PROP_HYDRO_184T(
            "Antminer S19 Pro+ Hyd. (184TH)",
            "antminer-s19-prop-hydro-184t"),

    /** Antminer S19 Pro+ Hydro (191T). */
    ANTMINER_S19_PROP_HYDRO_191T(
            "Antminer S19 Pro+ Hyd. (191TH)",
            "antminer-s19-prop-hydro-191t"),

    /** Antminer S19 Pro+ Hydro (198T). */
    ANTMINER_S19_PROP_HYDRO_198T(
            "Antminer S19 Pro+ Hyd. (198TH)",
            "antminer-s19-prop-hydro-198t"),

    /** Antminer S19 Pro. */
    ANTMINER_S19_PRO(
            "Antminer S19 Pro",
            "antminer-s19-pro"),

    /** Antminer S19 Pro (custom control board). */
    ANTMINER_S19_PRO_CUSTOM(
            "S19 Pro ",
            "antminer-s19-pro"),

    /** Antminer S19 Pro (105T). */
    ANTMINER_S19_PRO_105T(
            "Antminer S19 Pro (105T)",
            "antminer-s19-pro-105t"),

    /** Antminer S19 Pro (110T). */
    ANTMINER_S19_PRO_110T(
            "Antminer S19 Pro (110T)",
            "antminer-s19-pro-110t"),

    /** Antminer S19j. */
    ANTMINER_S19J(
            "Antminer S19j",
            "antminer-s19j"),

    /** Antminer S19j (82T). */
    ANTMINER_S19J_82T(
            "Antminer S19j (82T)",
            "antminer-s19j-82t"),

    /** Antminer S19j (86T). */
    ANTMINER_S19J_86T(
            "Antminer S19j (86T)",
            "antminer-s19j-86t"),

    /** Antminer S19j (90T). */
    ANTMINER_S19J_90T(
            "Antminer S19j (90T)",
            "antminer-s19j-90t"),

    /** Antminer S19j (94T). */
    ANTMINER_S19J_94T(
            "Antminer S19j (94T)",
            "antminer-s19j-94t"),

    /** Antminer S19j L. */
    ANTMINER_S19J_L(
            "Antminer S19j L",
            "antminer-s19j-l"),

    /** Antminer S19j L (86T). */
    ANTMINER_S19J_L_86T(
            "Antminer S19j L (86T)",
            "antminer-s19j-l-86t"),

    /** Antminer S19j L (90T). */
    ANTMINER_S19J_L_90T(
            "Antminer S19j L (90T)",
            "antminer-s19j-l-90t"),

    /** Antminer S19j Pro. */
    ANTMINER_S19J_PRO(
            "Antminer S19j Pro",
            "antminer-s19j-pro"),

    /** Antminer S19j Pro (BHB42601). */
    ANTMINER_S19J_PRO_BHB42601(
            "Antminer BHB42601",
            "antminer-s19j-pro"),

    /** Antminer S19j Pro (88T). */
    ANTMINER_S19J_PRO_88T(
            "Antminer S19j Pro (88T)",
            "antminer-s19j-pro-88t"),

    /** Antminer S19j Pro (92T). */
    ANTMINER_S19J_PRO_92T(
            "Antminer S19j Pro (92T)",
            "antminer-s19j-pro-92t"),

    /** Antminer S19j Pro (96T). */
    ANTMINER_S19J_PRO_96T(
            "Antminer S19j Pro (96T)",
            "antminer-s19j-pro-96t"),

    /** Antminer S19j Pro (98T). */
    ANTMINER_S19J_PRO_98T(
            "Antminer S19j Pro (98T)",
            "antminer-s19j-pro-98t"),

    /** Antminer S19j Pro (100T). */
    ANTMINER_S19J_PRO_100T(
            "Antminer S19j Pro (100T)",
            "antminer-s19j-pro-100t"),

    /** Antminer S19j Pro (104T). */
    ANTMINER_S19J_PRO_104T(
            "Antminer S19j Pro (104T)",
            "antminer-s19j-pro-104t"),

    /** Antminer S19j Pro-A. */
    ANTMINER_S19J_PRO_A(
            "Antminer S19j Pro-A",
            "antminer-s19j-pro-a"),

    /** Antminer S19j Pro-A (92T). */
    ANTMINER_S19J_PRO_A_92T(
            "Antminer S19j Pro-A (92T)",
            "antminer-s19j-pro-a-92t"),

    /** Antminer S19j Pro-A (96T). */
    ANTMINER_S19J_PRO_A_96T(
            "Antminer S19j Pro-A (96T)",
            "antminer-s19j-pro-a-96t"),

    /** Antminer S19j Pro-A (100T). */
    ANTMINER_S19J_PRO_A_100T(
            "Antminer S19j Pro-A (100T)",
            "antminer-s19j-pro-a-100t"),

    /** Antminer S19j Pro-A (104T). */
    ANTMINER_S19J_PRO_A_104T(
            "Antminer S19j Pro-A (104T)",
            "antminer-s19j-pro-a-104t"),

    /** Antminer S19 (vnish). */
    ANTMINER_S19_VNISH(
            "Antminer S19 (Vnish",
            "antminer-s19-vnish"),

    /** Antminer S19 Pro (vnish). */
    ANTMINER_S19_PRO_VNISH(
            "Antminer S19PRO (Vnish",
            "antminer-s19-pro-vnish"),

    /** Antminer S19j Pro (vnish). */
    ANTMINER_S19J_PRO_VNISH(
            "Antminer S19JPRO (Vnish",
            "antminer-s19j-pro-vnish"),

    /** Antminer S19j Pro BB (vnish). */
    ANTMINER_S19J_PRO_BB_VNISH(
            "Antminer S19JPRO BB (Vnish ",
            "antminer-s19j-pro-vnish"),

    /** Antminer S19j Pro AML (vnish). */
    ANTMINER_S19J_PRO_AML_VNISH(
            "Antminer S19JPRO AML (Vnish ",
            "antminer-s19j-pro-vnish"),

    /** Antminer S19j (vnish). */
    ANTMINER_S19J_VNISH(
            "Antminer S19J (Vnish",
            "antminer-s19j-vnish"),

    /** Antminer S19j Pro BB (vnish). */
    ANTMINER_S19J_BB_VNISH(
            "Antminer S19J BB (Vnish ",
            "antminer-s19j-vnish"),

    /** Antminer S19j Pro AML (vnish). */
    ANTMINER_S19J_AML_VNISH(
            "Antminer S19J AML (Vnish ",
            "antminer-s19j-vnish"),

    /** Antminer Z15. */
    ANTMINER_Z15(
            "Antminer Z15",
            "antminer-z15"),

    /** Antminer Z15J. */
    ANTMINER_Z15J(
            "Antminer Z15j",
            "antminer-z15j"),

    /** asicseer S9. */
    ASICSEER_S9(
            "Antminer S9",
            "antminer-s9"),

    /** asicseer S9i. */
    ASICSEER_S9i(
            "Antminer S9i",
            "antminer-s9i"),

    /** asicseer S9j. */
    ASICSEER_S9j(
            "Antminer S9j",
            "antminer-s9j"),

    /** asicseer T9. */
    ASICSEER_T9(
            "Antminer T9",
            "antminer-t9"),

    /** asicseer T9+. */
    ASICSEER_T9P(
            "Antminer T9+",
            "antminer-t9+"),

    /** asicseer T17. */
    ASICSEER_T17(
            "Antminer T17",
            "antminer-t17"),

    /** asicseer S17. */
    ASICSEER_S17(
            "Antminer S17",
            "antminer-t17"),

    /** asicseer S17. */
    ASICSEER_S17_PRO(
            "Antminer S17 Pro",
            "antminer-s17-pro"),

    /** Antminer G2. */
    ANTMINER_G2(
            "Antminer G2",
            "antminer-g2");

    /** The miner types, further refined by hash rates. */
    private static final Map<AntminerType, Map<String, AntminerType>> RATE_TYPES =
            new ConcurrentHashMap<>();

    /** All of the seer types. */
    private static final Map<String, AntminerType> SEER_TYPES =
            new ConcurrentHashMap<>();

    /** All of the types, by string, mapped to their type. */
    private static final Map<String, AntminerType> TYPE_MAP =
            new ConcurrentHashMap<>();

    static {
        for (final AntminerType asicType : values()) {
            if (!asicType.isSeer()) {
                TYPE_MAP.put(asicType.identifier, asicType);
                TYPE_MAP.put(asicType.alternate, asicType);
            } else {
                SEER_TYPES.put(asicType.identifier, asicType);
            }
        }

        // Antminer L7
        RATE_TYPES.put(
                ANTMINER_L7,
                ImmutableMap.of(
                        "5927.56",
                        ANTMINER_L7_8800,
                        "6099.37",
                        ANTMINER_L7_9050,
                        "6271.19",
                        ANTMINER_L7_9300,
                        "6357.09",
                        ANTMINER_L7_9500,
                        "8891.34",
                        ANTMINER_L7_8800,
                        "9149.06",
                        ANTMINER_L7_9050,
                        "9406.78",
                        ANTMINER_L7_9300,
                        "9535.64",
                        ANTMINER_L7_9500));

        // Antminer T19
        RATE_TYPES.put(
                ANTMINER_T19,
                ImmutableMap.of(
                        "84000.0",
                        ANTMINER_T19_84T,
                        "85226.0",
                        ANTMINER_T19_84T,
                        "88777.0",
                        ANTMINER_T19_88T));

        // Antminer T19 Hydro
        RATE_TYPES.put(
                ANTMINER_T19_HYDRO,
                ImmutableMap.of(
                        "136063.0",
                        ANTMINER_T19_HYDRO_119T,
                        "142542.0",
                        ANTMINER_T19_HYDRO_1255T,
                        "149021.0",
                        ANTMINER_T19_HYDRO_132T,
                        "155500.0",
                        ANTMINER_T19_HYDRO_1385T));

        // Antminer S19
        RATE_TYPES.put(
                ANTMINER_S19,
                ImmutableMap.of(
                        "82000.0",
                        ANTMINER_S19_82T,
                        "86000.0",
                        ANTMINER_S19_86T,
                        "90000.0",
                        ANTMINER_S19_90T,
                        "92328.0",
                        ANTMINER_S19_92T,
                        "95000.0",
                        ANTMINER_S19_95T,
                        "95879.0",
                        ANTMINER_S19_95T));

        // Antminer S19 Hydro
        RATE_TYPES.put(
                ANTMINER_S19_HYDRO,
                ImmutableMap.of(
                        "136063.0",
                        ANTMINER_S19_HYDRO_132T,
                        "142542.0",
                        ANTMINER_S19_HYDRO_1385T,
                        "149021.0",
                        ANTMINER_S19_HYDRO_145T,
                        "155500.0",
                        ANTMINER_S19_HYDRO_1515T));

        // Antminer S19 Pro
        RATE_TYPES.put(
                ANTMINER_S19_PRO,
                ImmutableMap.of(
                        "106533.0",
                        ANTMINER_S19_PRO_105T,
                        "110000.0",
                        ANTMINER_S19_PRO_110T,
                        "111859.0",
                        ANTMINER_S19_PRO_110T));

        // Antminer S19 Pro Hydro
        RATE_TYPES.put(
                ANTMINER_S19_PRO_HYDRO,
                ImmutableMap.of(
                        "154500.0",
                        ANTMINER_S19_PRO_HYDRO_1545T,
                        "162000.0",
                        ANTMINER_S19_PRO_HYDRO_162T,
                        "169500.0",
                        ANTMINER_S19_PRO_HYDRO_1695T));

        // Antminer S19 Pro+ Hydro
        RATE_TYPES.put(
                ANTMINER_S19_PROP_HYDRO,
                ImmutableMap.of(
                        "170000.0",
                        ANTMINER_S19_PROP_HYDRO_170T,
                        "177000.0",
                        ANTMINER_S19_PROP_HYDRO_177T,
                        "184000.0",
                        ANTMINER_S19_PROP_HYDRO_184T,
                        "191000.0",
                        ANTMINER_S19_PROP_HYDRO_191T,
                        "198000.0",
                        ANTMINER_S19_PROP_HYDRO_198T));

        // Antminer S19a
        RATE_TYPES.put(
                ANTMINER_S19A,
                ImmutableMap.of(
                        "92000.0",
                        ANTMINER_S19A_92T,
                        "92851.0",
                        ANTMINER_S19A_92T,
                        "93524.0",
                        ANTMINER_S19A_92T,
                        "96000.0",
                        ANTMINER_S19A_96T,
                        "96888.0",
                        ANTMINER_S19A_96T,
                        "97561.0",
                        ANTMINER_S19A_96T));

        // Antminer S19j
        RATE_TYPES.put(
                ANTMINER_S19J,
                ImmutableMap.of(
                        "82000.0",
                        ANTMINER_S19J_82T,
                        "86000.0",
                        ANTMINER_S19J_86T,
                        "90000.0",
                        ANTMINER_S19J_90T,
                        "94000.0",
                        ANTMINER_S19J_94T));

        // Antminer S19j L
        RATE_TYPES.put(
                ANTMINER_S19J_L,
                ImmutableMap.of(
                        "86000.0",
                        ANTMINER_S19J_L_86T,
                        "90000.0",
                        ANTMINER_S19J_L_90T));

        // Antminer S19a Pro
        RATE_TYPES.put(
                ANTMINER_S19A_PRO,
                ImmutableMap.of(
                        "112140.0",
                        ANTMINER_S19A_PRO_110T));

        // Antminer S19j Pro
        final Map<String, AntminerType> jProModels =
                ImmutableMap.of(
                        "88000.0",
                        ANTMINER_S19J_PRO_88T,
                        "92000.0",
                        ANTMINER_S19J_PRO_92T,
                        "96000.0",
                        ANTMINER_S19J_PRO_96T,
                        "98117.0",
                        ANTMINER_S19J_PRO_98T,
                        "100000.0",
                        ANTMINER_S19J_PRO_100T,
                        "104000.0",
                        ANTMINER_S19J_PRO_104T,
                        "105889.0",
                        ANTMINER_S19J_PRO_104T);
        RATE_TYPES.put(
                ANTMINER_S19J_PRO,
                jProModels);
        RATE_TYPES.put(
                ANTMINER_S19J_PRO_BHB42601,
                jProModels);

        // Antminer S19j Pro-A
        RATE_TYPES.put(
                ANTMINER_S19J_PRO_A,
                ImmutableMap.of(
                        "92000.0",
                        ANTMINER_S19J_PRO_A_92T,
                        "96000.0",
                        ANTMINER_S19J_PRO_A_96T,
                        "100000.0",
                        ANTMINER_S19J_PRO_A_100T,
                        "104000.0",
                        ANTMINER_S19J_PRO_A_104T));
    }

    /** The miner alternate identifier. */
    private final String alternate;

    /** The miner identifier. */
    private final String identifier;

    /** The miner ID associated with the miner in Foreman. */
    private final String slug;

    /**
     * Constructor.
     *
     * @param identifier The identifier.
     * @param alternate  The alternate identifier.
     * @param slug       The slug.
     */
    AntminerType(
            final String identifier,
            final String alternate,
            final String slug) {
        this.identifier = identifier;
        this.alternate = alternate;
        this.slug = slug;
    }

    /**
     * Constructor.
     *
     * @param identifier The identifier.
     * @param slug       The slug.
     */
    AntminerType(
            final String identifier,
            final String slug) {
        this(
                identifier,
                toAlternate(identifier),
                slug);
    }

    /**
     * Converts the provided model to an {@link AntminerType}.
     *
     * @param key           The key.
     * @param model         The model.
     * @param idealHashRate The ideal hash rate.
     *
     * @return The corresponding {@link AntminerType}.
     */
    public static Optional<AntminerType> forModel(
            final String key,
            final String model,
            final String idealHashRate) {
        AntminerType result = null;
        final String slug =
                key.contains("BOS")
                        ? key
                        : model;
        if (slug != null && !slug.isEmpty()) {
            final Optional<AntminerType> primary =
                    search(
                            slug,
                            AntminerType::getIdentifier,
                            idealHashRate);
            final Optional<AntminerType> secondary =
                    search(
                            toAlternate(slug),
                            AntminerType::getAlternate,
                            idealHashRate);
            result =
                    toBestCandidate(
                            primary.orElse(null),
                            secondary.orElse(null));
        }
        return Optional.ofNullable(result);
    }

    /**
     * Converts the provided model to an {@link AntminerType}.
     *
     * @param key   The key.
     * @param model The model.
     *
     * @return The corresponding {@link AntminerType}.
     */
    public static Optional<AntminerType> forModel(
            final String key,
            final String model) {
        return forModel(
                key,
                model,
                null);
    }

    /**
     * Converts the provided model to an {@link AntminerType}.
     *
     * @param type The type.
     *
     * @return The corresponding {@link AntminerType}.
     */
    public static Optional<AntminerType> forSeerModel(
            final String type) {
        if (type != null && !type.isEmpty()) {
            return SEER_TYPES.entrySet()
                    .stream()
                    .filter(entry -> type.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .max(Comparator.comparing(candidate -> candidate.getIdentifier().length()));
        }
        return Optional.empty();
    }

    /**
     * Returns the alternate identifier.
     *
     * @return The alternate identifier.
     */
    public String getAlternate() {
        return this.alternate;
    }

    @Override
    public Category getCategory() {
        return Category.ASIC;
    }

    /**
     * Returns the identifier.
     *
     * @return The identifier.
     */
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public String getSlug() {
        return this.slug;
    }

    /**
     * Returns whether or not the type indicates braiins os.
     *
     * @return Whether or not the type indicates braiins os.
     */
    public boolean isBraiins() {
        final String name = name().toLowerCase();
        return name.contains("bos") || name.contains("braiins");
    }

    /**
     * Returns whether or not the type indicates seer.
     *
     * @return Whether or not the type indicates seer.
     */
    public boolean isSeer() {
        final String name = name().toLowerCase();
        return name.contains("seer");
    }

    /**
     * Returns whether or not vnish.
     *
     * @return Whether or not vnish.
     */
    public boolean isVnish() {
        return this.identifier.toLowerCase().contains("vnish");
    }

    /**
     * Performs a search against the {@link #TYPE_MAP} for the most descriptive
     * candidate.
     *
     * @param slug          The slug.
     * @param identifier    The needle.
     * @param idealHashRate The ideal hash rate.
     *
     * @return The best result, if found.
     */
    private static Optional<AntminerType> search(
            final String slug,
            final Function<AntminerType, String> identifier,
            final String idealHashRate) {
        return TYPE_MAP.entrySet()
                .stream()
                .filter(entry -> slug.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .max(Comparator.comparing(type -> identifier.apply(type).length()))
                .map(antminerType -> {
                    if (idealHashRate != null) {
                        final Map<String, AntminerType> concreteTypes = RATE_TYPES.getOrDefault(antminerType, new HashMap<>());
                        return concreteTypes.getOrDefault(idealHashRate, antminerType);
                    }
                    return antminerType;
                });
    }

    /**
     * Creates an alternate from the provided candidate.
     *
     * @param candidate The candidate.
     *
     * @return The alternate.
     */
    private static String toAlternate(final String candidate) {
        return candidate.replace(" ", "").toLowerCase();
    }

    /**
     * Determines the best candidate (longest and most descriptive).
     *
     * @param candidate1 The first.
     * @param candidate2 The second.
     *
     * @return The best candidate.
     */
    private static AntminerType toBestCandidate(
            final AntminerType candidate1,
            final AntminerType candidate2) {
        if (candidate1 != null) {
            if (candidate2 != null) {
                return candidate1.slug.length() > candidate2.slug.length()
                        ? candidate1
                        : candidate2;
            }
            return candidate1;
        }
        return candidate2;
    }
}