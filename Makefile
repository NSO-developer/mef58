include setup.mk


#################
# Definitions
#################

DIRS = logs ncs-cdb state storedstate


#################
# Rules
#################

all: setup.mk packages netsim initial-data

MKDIR_P = mkdir -p

setup.mk:
	ncs-project update

$(DIRS):
	${MKDIR_P} ${DIRS}

cdb-clean:
	rm -rf ncs-cdb/*.cdb


netsim: packages $(DIRS)
	@if [ ! -e $@ ]; then \
          ncs-netsim create-device packages/oneaccess-tdre 1645-norway; \
          ncs-netsim add-device packages/oneaccess-tdre 1645-sweden; \
          ncs-netsim add-device packages/oneaccess-tdre 1645-finland; \
        fi
	ncs-netsim ncs-xml-init > ncs-cdb/netsim-devices.xml

.PHONY:initial-data
initial-data: $(DIRS)
	cp initial_data/mef-global.xml ncs-cdb/.
	cp initial_data/mef-interfaces.xml ncs-cdb/.
	cp initial_data/authgroups.xml ncs-cdb/.

demo-restart: qstop clean all start

clean: packages-clean netsim-clean
	rm -rf running.DB
	rm -rf *.trace
	rm -rf ${DIRS}
	rm -rf netsim

stop:
	-ncs --stop &
	-ncs-netsim stop

start: netsim-start ncs-start

ncs-start:
	ncs

netsim-start:
	ncs-netsim start

cli:
	ncs_cli -u admin -C

ncs-kill:
	pkill -lf ncs.conf

qstop:
	pkill -f ncs.conf || true
	pkill -f confd.conf || true

clean_cdb:
	rm -f ncs-cdb/*.cdb

