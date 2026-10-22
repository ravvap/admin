-- Local/test data taken from the mockup
INSERT INTO user_transaction_log
 (user_id, changed_at, cert_number, assessment_period, field_name, transaction_type, reason_code, reason_description, previous_value, current_value) VALUES
 ('AFOSTER',  '2025-11-25 14:11:00-05', 5072,  '2025 Q3', 'CEO Address 1',      'UPDATE', 'INSADRUPD', 'Updated general address',      '100 NORTH FRONT STREET', '100 N Front St'),
 ('PRAMESH',  '2025-11-20 11:40:00-05', 10626, '2025 Q3', 'Institution Profile','UPDATE', 'SIMSTRAN',  'SIMS transaction applied',   NULL,                     'SIMS TRAN 10626'),
 ('THOLLAND', '2025-11-19 10:20:00-05', 17589, '2025 Q3', 'Correspondence',     'UPDATE', 'MRGCORR',   'Merger correspondence update', NULL,                   'Merger notice sent 11/19/2025'),
 ('THOLLAND', '2025-11-19 10:19:00-05', 13584, '2025 Q3', 'Correspondence',     'UPDATE', 'MRGCORR',   'Merger correspondence update', NULL,                   'Merger notice sent 11/19/2025'),
 ('THOLLAND', '2025-11-19 10:18:00-05', 10460, '2025 Q3', 'Correspondence',     'UPDATE', 'MRGCORR',   'Merger correspondence update', NULL,                   'Merger notice sent 11/19/2025'),
 ('AFOSTER',  '2025-11-12 14:42:00-05', 34781, '2025 Q3', 'Record in INST_USER','CREATE', 'INSUSRADD', 'Added user comment',         NULL,                     'Address confirmed');
