# GlidePlugin

Paper-Plugin (1.21.x): Gleiten wie bei Craft Attack 14. Mit `/glide` schaltest du das Gleiten ein oder aus.
Gleiten startet erst, wenn du mindestens 18 Blöcke über dem Boden bist (einstellbar in `config.yml`).

## Befehl und Rechte

| Befehl   | Wirkung                          | Recht      |
|----------|----------------------------------|------------|
| `/glide` | Gleiten ein-/ausschalten         | `glide.use` (Standard: alle) |

## Konfiguration

```yaml
min-height: 18.0   # Mindesthöhe über dem Boden in Blöcken
```

## Bauen

Java 21 und Maven nötig:

```
mvn package
```

Die `.jar` liegt danach in `target/`. In den `plugins/`-Ordner des Paper-Servers kopieren und neu starten.
Bei jedem Push baut GitHub Actions die `.jar` außerdem automatisch (Reiter "Actions" > Build > Artifacts).
