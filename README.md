# Gestionnaire de dépenses

Application web Java 17 / Maven pour gérer les dépenses personnelles, les comptes, les catégories, les budgets, les imports CSV et les rapports.

## Prérequis

- Java 17
- Maven 3.9+
- MySQL 8
- Tomcat 10.1 ou équivalent pour Java EE / Jakarta Servlet 6
- Un certificat auto-signé pour HTTPS en environnement de développement

## Base de données

1. Créez la base MySQL :

```sql
CREATE DATABASE IF NOT EXISTS gestion_depenses CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Importez le schéma :

```bash
mysql -u <utilisateur> -p gestion_depenses < schema.sql
```

## Configuration

Copiez le fichier d'exemple en `config.properties` et adaptez les valeurs locales :

```bash
cp config.properties.example config.properties
```

Exemple de contenu :

```properties
db.url=jdbc:mysql://localhost:3306/gestion_depenses?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=depenses_app
db.password=********
```

La configuration peut également être fournie via la variable d'environnement `DEPENSES_CONFIG`.

## HTTPS en développement

Le lanceur Windows configure le connecteur HTTPS Tomcat sur le port 8443,
crée au besoin un certificat auto-signé pour `localhost`, et configure la
redirection de toutes les pages HTTP vers HTTPS. Il sauvegarde `server.xml`
avant toute modification et laisse le certificat dans le dossier de
configuration de Tomcat, hors du projet.

Pour générer manuellement un keystore de développement :

```bash
keytool -genkeypair -alias gestionnaire-depenses-local -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore tomcat-dev-keystore.p12 -storepass changeit -keypass changeit -validity 825 -dname "CN=localhost, OU=Dev, O=Gestionnaire de dépenses, C=FR" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

Le mot de passe `changeit` est réservé au certificat auto-signé de
développement. Ne réutilisez pas ce keystore ni ce mot de passe en production.
Le navigateur signalera que le certificat n'est pas émis par une autorité de
confiance ; c'est attendu en local. Utilisez `https://localhost:8443/` pour
éviter un décalage entre le nom du certificat et l'adresse du site.

## Lancement

### Compilation

```bash
mvn clean verify
```

### Déploiement sur Tomcat

Le projet contient un lanceur Windows qui compile, exécute les tests, configure HTTPS, déploie le WAR dans `C:\apache-tomcat-11.0.27` et vérifie HTTPS ainsi que la redirection HTTP :

1. Vérifiez que MySQL est démarré et que `config.properties` est présent à la racine du projet.
2. Double-cliquez sur `lancer-site.bat` et acceptez la demande d'autorisation administrateur Windows. Cette autorisation est nécessaire pour écrire dans le dossier Tomcat.
3. Le navigateur s'ouvre à l'adresse `https://localhost:8443/gestionnaire-depenses-1.0.0-SNAPSHOT/` si le déploiement réussit. Acceptez l'avertissement du certificat auto-signé pour le développement local.

Pour un déploiement manuel, configurez un connecteur HTTPS sur le port 8443 dans `server.xml`, définissez le port de redirection HTTP sur 8443, copiez le WAR dans le dossier `webapps` de Tomcat et définissez `DEPENSES_CONFIG` vers le fichier local `config.properties` avant de démarrer Tomcat. La contrainte de transport de l'application redirige les requêtes HTTP vers HTTPS ; le cookie de session est marqué `Secure` et `HttpOnly`.

### Démonstration publique sur Railway

Le `Dockerfile` du projet compile l'application avec Java 17, lance Tomcat 10.1 sur le port fourni par `PORT` et laisse Railway terminer HTTPS. Aucun fichier de configuration ou certificat local n'est inclus dans l'image. Les données doivent être fictives pour une démonstration.

1. Publiez le dépôt GitHub **après avoir vérifié que l'historique ne contient aucun mot de passe, certificat ou donnée personnelle**. Ne publiez pas `config.properties`.
2. Dans Railway, créez un projet, ajoutez un service **MySQL 8** et déployez le dépôt comme service Docker.
3. Dans le service MySQL, montez un volume persistant vers `/var/lib/mysql`.
4. Dans les variables du service Web, ajoutez les références de variables MySQL Railway :

```text
MYSQLHOST=${{MySQL.MYSQLHOST}}
MYSQLPORT=${{MySQL.MYSQLPORT}}
MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}
MYSQLUSER=${{MySQL.MYSQLUSER}}
MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}
DB_INITIALIZE_SCHEMA=true
```

Remplacez `MySQL` dans ces références par le nom exact donné au service de base de données dans votre projet Railway. `DB_INITIALIZE_SCHEMA=true` exécute le `schema.sql` idempotent au démarrage pour initialiser les tables et catégories par défaut ; retirez cette variable après l'initialisation. L'utilisateur MySQL doit pouvoir créer les tables dans la base qui lui a été attribuée.

5. Après le déploiement réussi, générez un domaine public pour le service Web. Railway fournit le HTTPS. Testez la connexion, créez un utilisateur de démonstration et ne transmettez au professeur que l'URL publique et des identifiants fictifs.
6. Quand le test est terminé, arrêtez/supprimez les services Railway et vérifiez la consommation du crédit d'essai et la persistance avant toute suppression.

Railway impose ses propres limites et son crédit d'essai peut être consommé avant la fin de la période d'essai. Consultez son tableau de consommation ; ne configurez aucun forfait payant si vous ne souhaitez pas engager de frais. Le schéma automatique ne remplace pas une stratégie de migration ou de sauvegarde pour un environnement de production.

## API REST interne

L'application expose des endpoints sous le préfixe `/api`, avec une réponse JSON uniforme :

```json
{ "succes": true, "donnees": { }, "erreur": null }
```

Endpoints principaux :

- `/api/auth/inscription`
- `/api/auth/connexion`
- `/api/auth/deconnexion`
- `/api/auth/moi`
- `/api/depenses`
- `/api/depenses-recurrentes`
- `/api/comptes`
- `/api/categories`
- `/api/budgets`
- `/api/import/csv/apercu`
- `/api/import/csv/valider`
- `/api/dashboard`
- `/api/rapports/mensuel`

## Sécurité

- Sessions HTTP
- Protection CSRF avec jeton dans l'en-tête `X-CSRF-Token`
- HSTS et en-têtes de sécurité HTTP activés dans les filtres
- Mot de passe haché via BCrypt
- Validation côté serveur des entrées

## Tests

Les tests JUnit couvrent :

- la catégorisation automatique par règles,
- le calcul d'état des budgets,
- le solde courant des comptes, y compris la conversion vers XOF,
- la conversion de devises vers XOF,
- la lecture des fichiers CSV délimités par virgule ou point-virgule.

## Notes

L'interface est en français et comprend les pages d'authentification, tableau de bord, dépenses, dépenses récurrentes, comptes, catégories, budgets, import CSV et rapports. L'historique des dépenses propose des filtres et une pagination par 25 lignes. Les routes API exigent une session, sauf inscription, connexion et consultation de session; les requêtes de modification sont protégées par jeton CSRF.

L'export PDF du rapport utilise la fonction d'impression du navigateur : choisissez « Enregistrer au format PDF » dans la fenêtre d'impression.
