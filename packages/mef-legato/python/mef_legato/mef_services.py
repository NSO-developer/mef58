# -*- mode: python; python-indent: 4 -*-
import ncs
from ncs.application import Service


# ------------------------
# SERVICE CALLBACK EXAMPLE
# ------------------------
class ServiceCallbacks(Service):

    # The create() callback is invoked inside NCS FASTMAP and
    # must always exist.
    @Service.create
    def cb_create(self, tctx, root, service, proplist):
        self.log.info('Service create(service=', service._path, ')')

        ############################
        #### Init
        self.tctx = tctx
        self.root = root
        self.service = service
        self.proplist = proplist
        self.template = ncs.template.Template(self.service)

        ############################
        ### Iterate over endpoints

        for ep in self.service.end_points.end_point:
            self.log.info("Endpoint: {}".format(ep.uni_id))
            self.setup_end_point(ep)


    def setup_end_point(self, ep):
        ############################
        ### Get interface
        intf = self.root.mef_interfaces.carrier_ethernet\
                        .subscriber_interfaces.uni[ep.uni_id]

        ### Get bandwidth profile
        bwp_name = ep.ingress_bwp_per_evc.bw_profile_flow_parameters
        bwp = self.root.mef_global.bwp_flow_parameter_profiles.profile[bwp_name]

        vars = ncs.template.Variables()
        vars.add('NID_DEVICE', intf.device)
        vars.add('BANDWIDTH_PROFILE', bwp_name)
        vars.add('CIR', bwp.cir)
        vars.add('EIR', bwp.eir)

        self.template.apply('mef-legato-services-end-point-template', vars)



    # The pre_modification() and post_modification() callbacks are optional,
    # and are invoked outside FASTMAP. pre_modification() is invoked before
    # create, update, or delete of the service, as indicated by the enum
    # ncs_service_operation op parameter. Conversely
    # post_modification() is invoked after create, update, or delete
    # of the service. These functions can be useful e.g. for
    #allocations that should be stored and existing also when the
    # service instance is removed.

    # @Service.pre_lock_create
    # def cb_pre_lock_create(self, tctx, root, service, proplist):
    #     self.log.info('Service plcreate(service=', service._path, ')')

    # @Service.pre_modification
    # def cb_pre_modification(self, tctx, op, kp, root, proplist):
    #     self.log.info('Service premod(service=', kp, ')')

    # @Service.post_modification
    # def cb_post_modification(self, tctx, op, kp, root, proplist):
    #     self.log.info('Service premod(service=', kp, ')')


# ---------------------------------------------
# COMPONENT THREAD THAT WILL BE STARTED BY NCS.
# ---------------------------------------------
class Main(ncs.application.Application):
    def setup(self):
        self.log.info('MEF-SERVICES RUNNING')

        self.register_service('mef-services', ServiceCallbacks)

    def teardown(self):
        self.log.info('MEF-SERVICES FINISHED')
