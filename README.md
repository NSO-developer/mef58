# Overview

This demo shows the use of the public MEF LSO Legato YANG modules and how to change the bandwidth profile for one endpoint.

The models are downloaded from the public MEF github repository at build time and augmented to tie them to the NSO device tree.


Git repository:

* https://github.com/MEF-GIT/YANG-public/tree/master/build

The MEF58 specification can be found here:

* https://www.mef.net/resources/technical-specifications/download?id=97&fileid=file1

The YANG modules including a service instance can be found here:

* https://www.mef.net/resources/technical-specifications/download?id=97&fileid=file3


The configation consists of:

 * Two global bandwidth profiles:
  - BANDWIDTH_LTE (cir 2000000 bits/sec)
  - BANDWIDTH_LTE2 (cir 3000000 bits/sec)

 * Three UNIs:
  - uni-kvantel (Norway)
  - uni-telia1 (Sweden)
  - uni-telia2 (Finland)

 * One EVC with three endpoints:
  - sd-wan
    - uni-kvantel (using bandwidth profile: BANDWIDTH_LTE)
    - uni-telia1 (using bandwidth profile: BANDWIDTH_LTE)
    - uni-telia2 (using bandwidth profile: BANDWIDTH_LTE)


# Demo

* Add MEF compatible configuration for NID devices.
* Provision mef-interfaces as part of the service configuration.
* Add topology configuration.
* Add MEF compatible configuration for backbone network devices.
* Add interface configuration to TDRE NED. Send configuration to device.


## Build

```
make all
make start
```

## Run
When you have started NSO, enter the cli and do a ```sync-from``` on the devices.

```
ncs_cli -u admin -g admin -C
devices device sync-from
```

### Show MEF global configuration (including bandwidth profiles)
```
show full-configuration mef-global
```
### Show UNIs
```
show full-configuration mef-interfaces
```

### Provision a multipoint EVC with three UNIs.
Merge in the prepare configuration and inspect it:
```
load merge initial_data/mef-services.xml
show configuration
```
Commit the configuration:
```
commit
```

### Update bandwidth profile for the norwegian endpoint
set mef-services carrier-ethernet subscriber-services evc sd-wan \
end-points end-point uni-kvantel ingress-bwp-per-evc \
bw-profile-flow-parameters BANDWIDTH_LTE2

### Show the correspondig commands to be sent to the device
```
commit dry-run outformat native
```
If it looks good, commit
```
commit
```
We can then activate the new settings
```
request devices device 1645-norway rpc \
rpc-activate-configuration activate-configuration
```