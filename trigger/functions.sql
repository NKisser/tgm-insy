CREATE OR REPLACE FUNCTION bilanz_mit_steuer(client_id INTEGER)
    RETURNS INTEGER
AS
$$
DECLARE
    einzahlungen DECIMAL;
    auszahlungen DECIMAL;
    einzahlungen2 DECIMAL;
    auszahlungen2 DECIMAL;
BEGIN
    SELECT SUM(amount) * 0.98 INTO einzahlungen FROM transfers WHERE to_client_id = client_id AND date < '2020-01-01';
    SELECT SUM(amount) * 0.98 INTO auszahlungen FROM transfers WHERE from_client_id = client_id AND date < '2020-01-01';

    SELECT SUM(amount) * 0.99 INTO einzahlungen2 FROM transfers WHERE to_client_id = client_id AND date >= '2020-01-01';
    SELECT SUM(amount) * 0.99 INTO auszahlungen2 FROM transfers WHERE from_client_id = client_id AND date >= '2020-01-01';

    RETURN (einzahlungen + einzahlungen2) - (auszahlungen + auszahlungen2);
END;
$$ LANGUAGE plpgsql;

SELECT bilanz_mit_steuer(1);
