CREATE TABLE garage_info (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50),
    location INT
);

INSERT INTO garage_info (name, location) VALUES
('GarageC', 740);


CREATE TABLE stock (
    id SERIAL PRIMARY KEY,
    item VARCHAR(50)
);

INSERT INTO stock (item) VALUES
('huile');
