# Machine specific TFAs

MACHINE_TFA_REQUIRE ?= ""
MACHINE_TFA_REQUIRE_fvp-base = "trusted-firmware-a-fvp.inc"
MACHINE_TFA_REQUIRE_fvp-base-arm32 = "trusted-firmware-a-fvp-arm32.inc"
MACHINE_TFA_REQUIRE_n1sdp = "trusted-firmware-a-n1sdp.inc"
MACHINE_TFA_REQUIRE_tc0 = "trusted-firmware-a-tc0.inc"

require ${MACHINE_TFA_REQUIRE}
