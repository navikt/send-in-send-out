CREATE TABLE payload
(
    reference_id        VARCHAR(256)		NOT NULL,
    content_id		    VARCHAR(256)		NOT NULL,
    content_type		VARCHAR(256)		NOT NULL,
    content             BYTEA		        NOT NULL,
    created_at		    TIMESTAMP		    DEFAULT now(),
    PRIMARY KEY (reference_id, content_id)
);

INSERT INTO payload (reference_id, content_id, content_type, content)
VALUES (
	'123',
	'a',
	'text/xml',
	'<?xml version="1.0" encoding="utf-8"?><dummy>xml 1</dummy>'
	), (
    '200',
    'b',
    'text/xml',
    '<?xml version="1.0" encoding="utf-8"?><dummy>xml 2</dummy>'
    ), (
    '200',
    'c',
    'text/xml',
    '<?xml version="1.0" encoding="utf-8"?><dummy>xml 3</dummy>'
    )