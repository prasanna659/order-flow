-- One database per service (no shared schema). Runs once, on first start of the postgres volume.
CREATE DATABASE authdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
