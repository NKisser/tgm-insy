CREATE OR REPLACE FUNCTION bilanz_mit_steuer (client_id INTEGER)
    RETURNS INTEGER
    AS $$
    DECLARE
       einzahlungen DECIMAL;
       auszahlungen DECIMAL;
    BEGIN
       SELECT SUM(amount) INTO einzahlungen FROM transfers WHERE to_client_id = client_id;
       SELECT SUM(amount) INTO auszahlungen FROM transfers WHERE from_client_id = client_id;
       RETURN einzahlungen - auszahlungen;
    END;
    $$ LANGUAGE plpgsql;

SELECT bilanz_mit_steuer(1);
