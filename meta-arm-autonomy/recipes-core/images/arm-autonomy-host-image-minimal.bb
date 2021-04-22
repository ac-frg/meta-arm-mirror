# Recipe to create a minimal Arm Autonomy stack host image

DESCRIPTION = "Arm Autonomy stack host minimal image"

# When alternate-kernel DISTRO_FEATURE is present we will build
# and install the alternate kernel
inherit ${@bb.utils.filter('DISTRO_FEATURES', 'alternate-kernel', d)}

inherit core-image features_check

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# The ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS variable can be used to include in the
# image one or several xenguest images.
# The list must be space separated and each entry must have the following
# format: URL[;params]
#  - URL can be the full path to a file or a Yocto compatible SRC_URI url
#  - params encompasses two values that can be optionally set:
#    - guestname=NAME can be used to specify the name of the guest. If not
#      specified the default value is the basename of the file
#      (without .xenguest extension).
#    - guestcount=NUM can be used to created NUM guests with the same config.
#      All guests after the first will have numbers appended to the guestname,
#      starting from 2. In the rootfs additional xenguest files will be
#      symlinks to the original.
#  params should be semicolon seperated, without a space, and can appear in
#  any order.
#
#  Here are examples of values:
#  /home/mydir/myguest.xenguest;guestname=guest1;guestcount=3
#  http://www.url.com/testguest.xenguest
#
#  If you are using the output of an other Yocto project, you should use the
#  full path syntax instead of the Yocto SRC_URI to be able to use the
#  symlink version of your image (as the real file has a new name on each
#  build as it includes the date). You must not use SRC_URI type file:// as
#  it will try to include the symlink and not the destination file which will
#  be detected by the recipe and output an error 'Guest file is a symlink'.
#
#  Guests can also be added using a bbapend to this recipe by adding entries
#  to SRC_URI with parameter ;guestname=NAME to specify the destination
#  guestname. The parameter guestname must be present as it is used to detect
#  guests to be added
ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS ??= ""

# Includes minimal set required to start and manage guest. The xen specific
# modules are not explicitly included as they are built as part of the kernel
# image for performance reasons. It doesn't include all kernel modules to
# reduce the image size. If the kernel-modules packages are needed they can
# be appended to IMAGE_INSTALL in a bbappend.
IMAGE_INSTALL += " \
    packagegroup-core-boot \
    packagegroup-core-ssh-openssh \
    qemu-system-i386 \
    xenguest-manager \
    xenguest-network \
    "

# Build xen binary
EXTRA_IMAGEDEPENDS += "xen"

# Build xen-devicetree to produce a xen ready devicetree
EXTRA_IMAGEDEPENDS += "xen-devicetree"

# Documentation for setting up a multiconfig build can be found in:
# meta-arm-autonomy/documentation/arm-autonomy-multiconfig.md

# In a multiconfig build this variable will hold a dependency string, which differs based
# on whether the guest has initramfs or not.
# It may have a space seperated list of dependency strings if mulitple guest types are
# configured
MC_DOIMAGE_MCDEPENDS ?= ""
# Example value: mc:host:guest:core-image-minimal:do_image_complete

# In a multiconfig build the host task 'do_image' has a dependency on multiconfig guest.
# This ensures that the guest image file already exists when it is needed by the host
DO_IMAGE_MCDEPENDS := "${@ '${MC_DOIMAGE_MCDEPENDS}' if d.getVar('BBMULTICONFIG') else ''}"

# Apply mc dependency. Empty string if multiconfig not enabled
do_image[mcdepends] += "${DO_IMAGE_MCDEPENDS}"

REQUIRED_DISTRO_FEATURES += 'arm-autonomy-host'
REQUIRED_DISTRO_FEATURES += 'xen'

python __anonymous() {
    import re
    guestfile_pattern = re.compile(r"^([^;]+);")
    guestname_pattern = re.compile(r";guestname=([^;]+);?")
    guestcount_pattern = re.compile(r";guestcount=(\d+);?")

    # Check in ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS for extra guests and add them
    # to SRC_URI with xenguest parameter if not set
    guestlist = d.getVar('ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS')
    if guestlist:
        for guest in guestlist.split():
            # If the user just specified a file instead of file://FILE, add
            # the file:// prefix
            if guest.startswith('/'):
                guestname = os.path.basename(guest)
                guestfile = guest
                guestcount = "1"
                f = guestfile_pattern.search(guest)
                n = guestname_pattern.search(guest)
                c = guestcount_pattern.search(guest)

                if f is not None:
                    guestfile = f.group(1)
                if n is not None:
                    guestname = n.group(1)
                if c is not None:
                    guestcount = c.group(1)
                # in case we have a link we need the destination
                guestfile = os.path.realpath(guestfile)

                # make sure the file exist to give a meaningfull error
                if not os.path.exists(guestfile):
                    raise bb.parse.SkipRecipe("ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS entry does not exist: " + guest)

                # In case the file is a symlink make sure we use the destination
                d.appendVar('SRC_URI',  ' file://' + guestfile + ';guestname=' + guestname + ';guestcount=' + guestcount)
            else:
                # we have a Yocto URL
                try:
                    _, _, path, _, _, parm = bb.fetch.decodeurl(guest)
                    # force guestname param in if not already there
                    if not 'guestname' in parm:
                        guest  += ';guestname=' + os.path.basename(path)
                    d.appendVar('SRC_URI', ' ' + guest)
                except:
                    raise bb.parse.SkipRecipe("ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS contains an invalid entry: " + guest)
}

python add_extern_guests () {
    # Destination directory on the rootfs
    guestdir = d.getVar('IMAGE_ROOTFS') + d.getVar('datadir') + '/guests'

    # Parse SRC_URI for files with ;guestname= parameter
    src_uri = d.getVar('SRC_URI')
    for entry in src_uri.split():
        _, _, path, _, _, parm = bb.fetch.decodeurl(entry)
        if 'guestname' in parm:
            if os.path.islink(path):
                realpath = os.path.realpath(path)

                if not os.path.exists(realpath):
                    bb.fatal("ARM_AUTONOMY_HOST_IMAGE_EXTERN_GUESTS link does not resolve: " + path)

                bb.note("Guest file is a symlink:\n " + path + "\nResolved to:\n " + realpath)
                path = realpath

            bb.utils.mkdirhier(guestdir)
            dstname = parm['guestname']
            # Add file extension if not there
            if not dstname.endswith('.xenguest'):
                dstname += '.xenguest'

            if not bb.utils.copyfile(path, guestdir + '/' + dstname):
                bb.fatal("Fail to copy Guest file " + path)

        if 'guestcount' in parm:
            guestcount = int(parm['guestcount']) + 1

            for i in range(2, guestcount):
                os.symlink('./' + dstname, guestdir + '/' + dstname.replace('.xenguest', str(i) + '.xenguest'))
}

IMAGE_PREPROCESS_COMMAND += "add_extern_guests; "

