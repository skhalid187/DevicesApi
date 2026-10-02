CREATE TABLE devices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    brand VARCHAR(100) NOT NULL CHECK (length(trim(brand)) > 0),
    state VARCHAR(20) NOT NULL CHECK (state IN ('AVAILABLE', 'IN_USE', 'INACTIVE')),
    creation_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_devices_brand_id ON devices (lower(brand), id);
CREATE INDEX idx_devices_state_id ON devices (state, id);

-- Preserve the audit timestamp even if a future application change issues direct SQL.
CREATE FUNCTION prevent_device_creation_time_update() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.creation_time IS DISTINCT FROM OLD.creation_time THEN
        RAISE EXCEPTION 'Device creation_time is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER devices_immutable_creation_time
    BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION prevent_device_creation_time_update();
