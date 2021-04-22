FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://0001-vif-nat-fix-hostname.patch \
           "

PACKAGECONFIG_remove = "\
    ${@bb.utils.contains('DISTRO_FEATURES', \
                         'arm-autonomy-host', \
                         'sdl', '', d)}"
