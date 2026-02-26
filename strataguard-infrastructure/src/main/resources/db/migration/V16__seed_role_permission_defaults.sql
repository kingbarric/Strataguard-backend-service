-- V16: Seed default role-to-permission mappings

-- SUPER_ADMIN: ALL permissions (platform scope, can do everything)
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'SUPER_ADMIN', unnest(ARRAY[
    'estate.read','estate.create','estate.update','estate.delete',
    'unit.read','unit.create','unit.update','unit.delete',
    'tenancy.read','tenancy.create','tenancy.update','tenancy.terminate',
    'resident.read','resident.create','resident.update','resident.delete','resident.link_user',
    'vehicle.read','vehicle.create','vehicle.update','vehicle.delete','vehicle.import',
    'charge.read','charge.create','charge.update','charge.delete','charge.exclusion_manage',
    'levy.create','levy.generate','penalty.apply',
    'invoice.read','invoice.create','invoice.update','invoice.void','invoice.export','invoice.summary',
    'payment.read','payment.initiate','payment.verify','payment.refund','payment.export',
    'wallet.read','wallet.fund','wallet.transfer',
    'gate.entry','gate.exit','gate.session_read','gate.log_read','gate.manual_override',
    'exit_pass.generate','exit_approval.create','exit_approval.manage',
    'visitor.read','visitor.create','visitor.update','visitor.delete',
    'visitor.checkin','visitor.checkout','visitor.all_read',
    'visit_pass.read','visit_pass.create','visit_pass.revoke',
    'blacklist.read','blacklist.create','blacklist.update','blacklist.delete','blacklist.check',
    'incident.read','incident.create','incident.update','incident.close',
    'cctv.read','cctv.manage','cctv.live_feed',
    'patrol.read','patrol.manage','patrol.scan',
    'emergency.read','emergency.create','emergency.acknowledge',
    'staff.read','staff.manage',
    'maintenance.read','maintenance.create','maintenance.assign','maintenance.close','maintenance.comment','maintenance.rate',
    'amenity.read','amenity.create','amenity.update','amenity.delete',
    'booking.read','booking.create','booking.approve','booking.cancel',
    'utility.meter_read','utility.meter_manage','utility.reading_create','utility.reading_read','utility.cost_manage',
    'artisan.read','artisan.create','artisan.update','artisan.delete','artisan.rate','artisan.verify',
    'announcement.read','announcement.create','announcement.update','announcement.delete','announcement.publish',
    'poll.read','poll.create','poll.update','poll.delete','poll.vote','poll.close',
    'violation.read','violation.create','violation.update','violation.close','violation.appeal','violation.appeal_review',
    'complaint.read','complaint.create','complaint.update','complaint.assign','complaint.close','complaint.escalate',
    'notification.read','notification.send','notification.template_manage','notification.preference_manage',
    'chat.read','chat.send',
    'report.financial','report.security','report.maintenance','report.occupancy','report.gate',
    'audit.read','audit.export',
    'membership.read','membership.create','membership.update','membership.delete',
    'portfolio.read','portfolio.create','portfolio.update','portfolio.delete','portfolio.assign_estate'
]);

-- ESTATE_ADMIN: Full estate operations
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'ESTATE_ADMIN', unnest(ARRAY[
    'estate.read','estate.update',
    'unit.read','unit.create','unit.update','unit.delete',
    'tenancy.read','tenancy.create','tenancy.update','tenancy.terminate',
    'resident.read','resident.create','resident.update','resident.delete','resident.link_user',
    'vehicle.read','vehicle.create','vehicle.update','vehicle.delete','vehicle.import',
    'charge.read','charge.create','charge.update','charge.delete','charge.exclusion_manage',
    'levy.create','levy.generate','penalty.apply',
    'invoice.read','invoice.create','invoice.update','invoice.void','invoice.export','invoice.summary',
    'payment.read','payment.verify','payment.export',
    'wallet.read',
    'gate.session_read','gate.log_read','gate.manual_override',
    'exit_approval.manage',
    'visitor.read','visitor.all_read',
    'visit_pass.read','visit_pass.revoke',
    'blacklist.read','blacklist.create','blacklist.update','blacklist.delete','blacklist.check',
    'incident.read','incident.create','incident.update','incident.close',
    'cctv.read','cctv.manage','cctv.live_feed',
    'patrol.read','patrol.manage',
    'emergency.read','emergency.acknowledge',
    'staff.read','staff.manage',
    'maintenance.read','maintenance.assign','maintenance.close','maintenance.comment',
    'amenity.read','amenity.create','amenity.update','amenity.delete',
    'booking.read','booking.approve','booking.cancel',
    'utility.meter_read','utility.meter_manage','utility.reading_create','utility.reading_read','utility.cost_manage',
    'artisan.read','artisan.create','artisan.update','artisan.delete','artisan.verify',
    'announcement.read','announcement.create','announcement.update','announcement.delete','announcement.publish',
    'poll.read','poll.create','poll.update','poll.delete','poll.close',
    'violation.read','violation.create','violation.update','violation.close','violation.appeal_review',
    'complaint.read','complaint.update','complaint.assign','complaint.close','complaint.escalate',
    'notification.read','notification.send','notification.template_manage',
    'chat.read','chat.send',
    'report.financial','report.security','report.maintenance','report.occupancy','report.gate',
    'audit.read','audit.export',
    'membership.read','membership.create','membership.update','membership.delete'
]);

-- FINANCE_OFFICER
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'FINANCE_OFFICER', unnest(ARRAY[
    'estate.read',
    'unit.read',
    'tenancy.read',
    'resident.read',
    'charge.read','charge.create','charge.update',
    'charge.exclusion_manage',
    'levy.create','levy.generate','penalty.apply',
    'invoice.read','invoice.create','invoice.update','invoice.void','invoice.export','invoice.summary',
    'payment.read','payment.verify','payment.export',
    'wallet.read',
    'report.financial',
    'notification.read','notification.send',
    'audit.read'
]);

-- SECURITY_MANAGER
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'SECURITY_MANAGER', unnest(ARRAY[
    'estate.read',
    'gate.entry','gate.exit','gate.session_read','gate.log_read','gate.manual_override',
    'exit_pass.generate','exit_approval.create','exit_approval.manage',
    'visitor.read','visitor.all_read','visitor.checkin','visitor.checkout',
    'visit_pass.read',
    'blacklist.read','blacklist.create','blacklist.update','blacklist.delete','blacklist.check',
    'incident.read','incident.create','incident.update','incident.close',
    'cctv.read','cctv.manage','cctv.live_feed',
    'patrol.read','patrol.manage','patrol.scan',
    'emergency.read','emergency.create','emergency.acknowledge',
    'staff.read','staff.manage',
    'report.security','report.gate',
    'announcement.read',
    'audit.read'
]);

-- SECURITY_GUARD
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'SECURITY_GUARD', unnest(ARRAY[
    'estate.read',
    'gate.entry','gate.exit','gate.session_read','gate.log_read',
    'exit_pass.generate','exit_approval.create',
    'visitor.read','visitor.checkin','visitor.checkout',
    'visit_pass.read',
    'blacklist.read','blacklist.check',
    'incident.read','incident.create',
    'patrol.read','patrol.scan',
    'emergency.read','emergency.create',
    'announcement.read'
]);

-- FACILITY_MANAGER
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'FACILITY_MANAGER', unnest(ARRAY[
    'estate.read',
    'unit.read',
    'tenancy.read',
    'resident.read',
    'charge.read',
    'invoice.read',
    'maintenance.read','maintenance.create','maintenance.assign','maintenance.close','maintenance.comment',
    'amenity.read','amenity.create','amenity.update',
    'booking.read','booking.approve','booking.cancel',
    'utility.meter_read','utility.meter_manage','utility.reading_create','utility.reading_read','utility.cost_manage',
    'artisan.read','artisan.create','artisan.update',
    'announcement.read','announcement.create','announcement.update',
    'poll.read','poll.create','poll.update',
    'violation.read','violation.create','violation.update',
    'complaint.read','complaint.update','complaint.assign','complaint.close',
    'gate.session_read','gate.log_read',
    'report.maintenance','report.occupancy',
    'notification.read','notification.send',
    'chat.read','chat.send'
]);

-- FRONT_DESK
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'FRONT_DESK', unnest(ARRAY[
    'estate.read',
    'resident.read',
    'visitor.read','visitor.create','visitor.checkin','visitor.checkout',
    'visit_pass.read',
    'announcement.read',
    'blacklist.check'
]);

-- RESIDENT_PRIMARY
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'RESIDENT_PRIMARY', unnest(ARRAY[
    'estate.read',
    'unit.read',
    'tenancy.read',
    'resident.read',
    'vehicle.read','vehicle.create','vehicle.update',
    'invoice.read',
    'payment.read','payment.initiate',
    'wallet.read','wallet.fund','wallet.transfer',
    'exit_pass.generate',
    'visitor.read','visitor.create','visitor.update','visitor.delete',
    'visit_pass.read','visit_pass.create','visit_pass.revoke',
    'maintenance.read','maintenance.create','maintenance.comment','maintenance.rate',
    'amenity.read',
    'booking.read','booking.create','booking.cancel',
    'utility.reading_read',
    'artisan.read','artisan.rate',
    'announcement.read',
    'poll.read','poll.vote',
    'violation.read','violation.appeal',
    'complaint.read','complaint.create',
    'notification.read','notification.preference_manage',
    'chat.read','chat.send',
    'charge.read'
]);

-- RESIDENT_DEPENDENT
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'RESIDENT_DEPENDENT', unnest(ARRAY[
    'estate.read',
    'unit.read',
    'visitor.read','visitor.create',
    'visit_pass.read','visit_pass.create',
    'exit_pass.generate',
    'amenity.read',
    'booking.read','booking.create',
    'maintenance.read','maintenance.create',
    'announcement.read',
    'poll.read','poll.vote',
    'notification.read','notification.preference_manage',
    'chat.read','chat.send'
]);

-- PORTFOLIO_ADMIN (Phase 6 roles, seeded now for completeness)
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'PORTFOLIO_ADMIN', unnest(ARRAY[
    'portfolio.read','portfolio.update','portfolio.assign_estate',
    'estate.read','estate.create',
    'membership.read','membership.create','membership.update',
    'report.financial','report.occupancy',
    'audit.read'
]);

-- PORTFOLIO_VIEWER
INSERT INTO role_permission_defaults (id, role, permission)
SELECT uuid_generate_v4(), 'PORTFOLIO_VIEWER', unnest(ARRAY[
    'portfolio.read',
    'estate.read',
    'report.financial','report.occupancy'
]);
