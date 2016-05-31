-- PermissionsEx Schema v3, PostgreSQL Edition

-- Data Types Used
-- ---------------
-- Identifier: varchar(255)
-- Permission value: smallint
-- Unique ids: int(11)
-- Text values: text

CREATE TABLE "{}global" (
key VARCHAR(255) PRIMARY KEY,
value TEXT NOT NULL
);

CREATE TABLE "{}subjects" (
id int NOT NULL PRIMARY KEY ,
"type" varchar(255) DEFAULT NULL,
"name" varchar(255) DEFAULT NULL,
PRIMARY KEY ("id"),
UNIQUE KEY "ident" ("type","name"),
KEY "type_k" ("type")
);

CREATE TABLE "{}segments" (
"id" int NOT NULL PRIMARY KEY ,
"subject" int NOT NULL,
"perm_default" SMALLINT DEFAULT NULL,
"weight" int NOT NULL,
"inheritable" BOOLEAN NOT NULL,
PRIMARY KEY ("id"),
KEY "subject" ("subject"),
FOREIGN KEY ("subject") REFERENCES "{}subjects" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE "{}permissions" (
"segment" int NOT NULL,
"key" varchar(255) DEFAULT NULL,
"value" smallint DEFAULT NULL,
UNIQUE ("segment","key"),
FOREIGN KEY ("segment") REFERENCES "{}segments" ("id") ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE "{}contexts" (
"segment" int NOT NULL,
"key" varchar(255) NOT NULL,
"value" varchar(255) NOT NULL,
UNIQUE ("segment","key"),
UNIQUE ("segment","key","value"),
CONSTRAINT "contexts_ibfk_1" FOREIGN KEY ("segment") REFERENCES "segments" ("id") ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE "{}options" (
"segment" int(11) NOT NULL,
"key" varchar(255) DEFAULT NULL,
"value" text,
UNIQUE ("segment","key"),
CONSTRAINT "options_ibfk_1" FOREIGN KEY ("segment") REFERENCES "segments" ("id") ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE "{}inheritance" (
"segment" int(11) NOT NULL,
"parent" int(11) NOT NULL,
UNIQUE ("segment","parent"),
KEY "parent" ("parent"),
CONSTRAINT "inheritance_ibfk_1" FOREIGN KEY ("segment") REFERENCES "segments" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
CONSTRAINT "inheritance_ibfk_2" FOREIGN KEY ("parent") REFERENCES "subjects" ("id") ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE "{}rank_ladders" (
"id" int(11) NOT NULL AUTO_INCREMENT,
"name" varchar(255) DEFAULT NULL,
"subject" int(11) DEFAULT NULL,
PRIMARY KEY ("id"),
UNIQUE ("name","subject"),
KEY "subject" ("subject"),
FOREIGN KEY ("subject") REFERENCES "subjects" ("id") ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE "{}context_inheritance" (
"id" int(11) NOT NULL AUTO_INCREMENT,
"child_key" varchar(255) DEFAULT NULL,
"child_value" varchar(255) DEFAULT NULL,
"parent_key" varchar(255) DEFAULT NULL,
"parent_value" varchar(255) DEFAULT NULL,
PRIMARY KEY ("id"),
UNIQUE ("child_key","child_value","parent_key","parent_value"),
KEY "child_key" ("child_key","child_value"),
KEY "parent_key" ("parent_key","parent_value")
) DEFAULT CHARSET=utf8;
