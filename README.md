# POC JMS – Client et Garages

Ce projet est un **POC** démontrant la partie communication entre le client et les garages via **JMS**.
Le client envoie une demande (ex : "pneu:30") et les garages répondent avec la distance entre eux et le client. Le client reçoit les réponses et chosiit automatiquement le garage le plus proche.

---

## Prérequis

* Docker & Docker Compose installés
* Le projet utilise des images pré-compilées via Docker

---

## Structure du projet

```
.
├── client/       # Application
├── garagea/      # Garage A 
├── garageb/      # Garage B 
├── garagec/      # Garage C 
└── docker-compose.yml
```

---

## Lancer le POC

1. **Arrêter et nettoyer les containers précédents **
   (J'ai déjà eu de la merde avec JMS qui ne veut pas se lancer jsp pourquoi)

```bash
docker-compose down -v
```

2. **Lancer**

```bash
docker-compose up --build -d
```

3. **Lire les logs pour observer les échanges client <-> garages**
  Les logs important sont ceux du client (on y voit les résultats), le reste c'est pour debugs.
  
* Client :

```bash
docker logs -f poc_jms_stiz_gie-client-1
```

* Garages :

```bash
docker logs -f garagea
docker logs -f garageb
docker logs -f garagec
```

* ActiveMQ:

```bash
docker logs -f activemq
```

---

## Fonctionnement du POC

1. Le **client** envoie une demande de service (`pneu:30`) à tous les garages via JMS.
2. Chaque **garage** vérifie son stock et calcule la distance entre sa localisation et celle du client.
3. Chaque garage renvoie sa réponse sur une queue **réponse privée du client**.
4. Le client reçoit toutes les réponses, sélectionne la meilleure (distance minimale) et l’affiche dans les logs.

