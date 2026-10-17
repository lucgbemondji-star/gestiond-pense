CREATE DATABASE IF NOT EXISTS gestion_depenses CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestion_depenses;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS utilisateur (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_utilisateur_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS compte (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    nom VARCHAR(120) NOT NULL,
    type ENUM('ESPECES', 'MOBILE_MONEY', 'BANQUE') NOT NULL,
    devise VARCHAR(10) NOT NULL,
    solde_initial DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY idx_compte_utilisateur (utilisateur_id),
    CONSTRAINT fk_compte_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categorie (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NULL,
    nom VARCHAR(120) NOT NULL,
    parent_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_categorie_utilisateur (utilisateur_id),
    KEY idx_categorie_parent (parent_id),
    CONSTRAINT fk_categorie_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_categorie_parent FOREIGN KEY (parent_id) REFERENCES categorie(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS depense (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    compte_id BIGINT NOT NULL,
    categorie_id BIGINT NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    devise VARCHAR(10) NOT NULL,
    montant_xof DECIMAL(15,2) NOT NULL,
    date_depense DATE NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    note VARCHAR(500) NULL,
    recurrente_id BIGINT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_depense_utilisateur_date (utilisateur_id, date_depense),
    KEY idx_depense_compte (compte_id),
    KEY idx_depense_categorie (categorie_id),
    CONSTRAINT fk_depense_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_depense_compte FOREIGN KEY (compte_id) REFERENCES compte(id) ON DELETE RESTRICT,
    CONSTRAINT fk_depense_categorie FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS depense_recurrente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    compte_id BIGINT NOT NULL,
    categorie_id BIGINT NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    frequence ENUM('HEBDO', 'MENSUEL') NOT NULL,
    prochaine_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_depense_recurrente_utilisateur (utilisateur_id),
    CONSTRAINT fk_recurrente_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrente_compte FOREIGN KEY (compte_id) REFERENCES compte(id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurrente_categorie FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS budget (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    categorie_id BIGINT NOT NULL,
    periode ENUM('HEBDO', 'MENSUEL') NOT NULL,
    plafond_xof DECIMAL(15,2) NOT NULL,
    date_debut DATE NOT NULL,
    PRIMARY KEY (id),
    KEY idx_budget_utilisateur (utilisateur_id),
    CONSTRAINT fk_budget_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_categorie FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS regle_categorie (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    mot_cle VARCHAR(80) NOT NULL,
    categorie_id BIGINT NOT NULL,
    poids INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_regle_utilisateur (utilisateur_id),
    KEY idx_regle_categorie (categorie_id),
    CONSTRAINT fk_regle_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_regle_categorie_ref FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO categorie (id, utilisateur_id, nom, parent_id) VALUES
    (1, NULL, 'Alimentation', NULL),
    (2, NULL, 'Transport', NULL),
    (3, NULL, 'Logement', NULL),
    (4, NULL, 'Santé', NULL),
    (5, NULL, 'Éducation', NULL),
    (6, NULL, 'Loisirs', NULL),
    (7, NULL, 'Communication', NULL),
    (8, NULL, 'Autres', NULL)
ON DUPLICATE KEY UPDATE nom = VALUES(nom), utilisateur_id = VALUES(utilisateur_id), parent_id = VALUES(parent_id);

ALTER TABLE categorie AUTO_INCREMENT = 9;
