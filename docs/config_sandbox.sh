#!/bin/sh

# This file provides sample configuration scripting for setting up
# password-less logins on the sandbox (or other VM). Follow the general
# nature of the commands and the Monitor war sould work relative to Xen
# or KVM type hypervisors.

# Before doing any of this, you should clean up the servers to which
# the keys ssh-copy-id will copy. The servers will already have 'stale' keys
# from a prior sandbox (or other VM), and those should be removed from the
# relevant files (usually ~/.ssh/authorized_keys on the remote machine).

ssh-keygen -t rsa
ssh-copy-id -i ~/.ssh/id_rsa.pub root@msicloud2.momentumsoftware.com
ssh-copy-id -i ~/.ssh/id_rsa.pub root@msicloud4.momentumsoftware.com
ssh-copy-id -i ~/.ssh/id_rsa.pub root@msicloud9.momentumsoftware.com
