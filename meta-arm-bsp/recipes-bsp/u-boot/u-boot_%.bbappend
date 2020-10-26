# Machine specific u-boot

THIS_DIR := "${THISDIR}"
FILESEXTRAPATHS_prepend = "${THIS_DIR}/${BP}:"
FILESEXTRAPATHS_prepend_fvp-base = "${THIS_DIR}/${BP}/fvp-common:"
FILESEXTRAPATHS_prepend_foundation-armv8 = "${THIS_DIR}/${BP}/fvp-common:"

#
# Cortex-A5 DesignStart KMACHINE
#
SRC_URI_append_a5ds = " file://0001-armv7-add-mmio-timer.patch \
                        file://0002-board-arm-add-designstart-cortex-a5-board.patch"

#
# FVP FOUNDATION KMACHINE
#
SRC_URI_append_foundation-armv8 = " file://u-boot_vexpress_fvp.patch"

#
# FVP BASE KMACHINE
#
SRC_URI_append_fvp-base = " file://u-boot_vexpress_fvp.patch"

#
# FVP BASE ARM32 KMACHINE
#
SRC_URI_append_fvp-base-arm32 = " file://0001-Add-vexpress_aemv8a_aarch32-variant.patch"

#
# Juno KMACHINE
#
SRC_URI_append_juno = " file://u-boot_vexpress_uenv.patch"

#
# Total Compute KMACHINE
#
SRC_URI_tc0 = "git://git.denx.de/u-boot.git \
          "
SRCREV_tc0 = "565add124de00c994652a0d2d6d1eb6b2a7c9553"
LIC_FILES_CHKSUM_tc0 = "file://Licenses/README;md5=5a7450c57ffe5ae63fd732446b988025"
#
# Corstone700 KMACHINE
#
FILESEXTRAPATHS_prepend_corstone700 := "${THISDIR}/files/corstone700:"
 
SRC_URI_append_corstone700 = " file://0001-arm-Add-corstone700-platform.patch \
                               file://0002-boot-add-bootx-command-to-start-XiP-images.patch \
                               file://0003-boot-starting-the-XIP-kernel-using-bootx-command.patch \
                               file://0004-arm-enabling-the-arch_timer-driver.patch"
