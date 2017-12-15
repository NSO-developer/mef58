all: fxs
.PHONY: all

MEFYANGS = yang/mef-global.yang yang/mef-legato-interfaces.yang yang/mef-legato-services.yang yang/mef-types.yang


yang/mef-global.yang:
	wget -O $@ https://raw.githubusercontent.com/MEF-GIT/YANG-public/master/build/mef-global%402017-08-30.yang

yang/mef-legato-interfaces.yang:
	wget -O $@ https://raw.githubusercontent.com/MEF-GIT/YANG-public/master/build/mef-legato-interfaces%402017-08-30.yang

yang/mef-legato-services.yang:
	wget -O $@ https://raw.githubusercontent.com/MEF-GIT/YANG-public/master/build/mef-legato-services%402017-08-30.yang
	perl -pi -e \
'$$_=qq(  import tailf-ncs {\n    prefix ncs;\n  }\n\n).$$_ if /organization "MEF Forum";/;'\
'$$_.=qq(\n          uses ncs:service-data;\n          ncs:servicepoint "mef-services";\n\n) if /key "evc-id";/;'\
 $@

yang/mef-types.yang:
	wget -O $@ https://raw.githubusercontent.com/MEF-GIT/YANG-public/master/build/mef-types%402017-08-30.yang


# Include standard NCS examples build definitions and rules
include $(NCS_DIR)/src/ncs/build/include.ncs.mk

SRC  = $(wildcard yang/*.yang)
SRC += $(MEFYANGS)
DIRS = ../load-dir
FXS = $(SRC:yang/%.yang=../load-dir/%.fxs)

## Uncomment and patch the line below if you have a dependency to a NED
## or to other YANG files
# YANGPATH += ../../<ned-name>/src/ncsc-out/modules/yang \
# 	../../<pkt-name>/src/yang

YANGPATH += ./yang

NCSCPATH   = $(YANGPATH:%=--yangpath %)
YANGERPATH = $(YANGPATH:%=--path %)

fxs: $(MEFYANGS) $(DIRS) $(FXS)
.PHONY: fxs

$(DIRS):
	mkdir -p $@

../load-dir/%.fxs: yang/%.yang
	$(NCSC)  `ls $*-ann.yang  > /dev/null 2>&1 && echo "-a $*-ann.yang"` \
             $(NCSCPATH) -c -o $@ $<
clean:
	rm -rf $(DIRS)
	rm -f $(MEFYANGS)
.PHONY: clean
