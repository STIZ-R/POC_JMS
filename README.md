# POC JMS – Client, Garages et ETL

Ce projet est un **POC** démontrant :

1. La communication entre le client et les garages via **JMS**.
2. L’agrégation des stocks des garages dans une base **PostgreSQL analytique** via un **ETL** et obtenir la liste des objets manquants pour, in fine, les envoyer pour les usines.

Le client envoie une demande (ex : "pneu:30") et les garages répondent avec leur stock et la distance au client. Le client choisit automatiquement le garage le plus proche. Ensuite, l’**ETL** consolide toutes les données des garages dans la base analytique afin qu'un algorithme décide des stocks manquants.

---

## Prérequis

* Docker & Docker Compose installés
* Le projet utilise des images pré-compilées via Docker

---

## Structure du projet

```
.
├── client/       # Application client 
├── garagea/      # Garage A
├── garageb/      # Garage B
├── garagec/      # Garage C
├── etl/          # ETL pour aggréger les 3 BDs des garages
└── docker-compose.yml
```

---

## Lancer le POC

1. **Arrêter et nettoyer les containers précédents**
    J'ai déjà eu des problèmes lors de la lecture des logs, alors n'hésitez pas à down + up plusieurs fois
```bash
docker-compose down -v
```

2. **Lancer tous les services**

```bash
docker-compose up --build -d
```

3. **Lire les logs pour observer les échanges et l’ETL**

* Client (demandes et réponses) :

Ce sont les logs principaux pour obtenir les résultats de 'JMS'
```bash
docker logs -f client
```

* ETL (consolidation des stocks) :

Ce sont les résultats pour voir ce qu'il manque dans la BD des garages
```bash
docker logs -f etl
```

---

## Fonctionnement

1. **Client → Garages via JMS** : le client envoie une demande de service (`pneu:30`) à tous les garages.
2. **Garages → Client** : chaque garage vérifie son stock et la distance au client, puis renvoie la réponse.
3. **Client** : reçoit toutes les réponses, sélectionne la meilleure (distance minimale) et affiche le résultat dans les logs.
4. **ETL** : se connecte à toutes les bases des garages (garagea-db, garageb-db, garagec-db) et aggrège les données dans la base analytique.
5. **Analyse**: Nous pouvons ensuite faire tourner un algorithme qui décide de ce qu'il manque dans la BD.