# Journal IA — Kilasy

Outil utilisé : GitHub Copilot, dans IntelliJ IDEA

## Changement C1 : Pénalité de retard

### Échange 1 — 17:47 25/11
Demande :

    Propose une méthode penalizedGrade(raw, max, deadline, submittedDay) 
    en BigDecimal, arrondi à 1 décimale HALF_UP, qui retourne la note finale sur 20, 
    sans nouvelle classe et sans corriger les codes

Proposition :

    une méthode qui compte les jours de retard avec ChronoUnit.DAYS.between 
    (0 si rendu à l'heure), calcule la pénalité avec 
    BigDecimal.valueOf(Math.min(lateDays, 5), 1) 
    (donc 0,1 par jour, plafonné à 0,5), puis raw × 20 × (1 − pénalité) ÷ max
    Elle lève une exception si max <= 0
Décision : `gardé, en mettant les méthodes en private et pas juste static`

Pourquoi :

    le calcul est correct, 
    le plafond de 50 % passe par Math.min(lateDays, 5), 
    et la note ne peut pas être négative (pénalité ≤ 0,5 et points validés entre 0 et le barème)

Vérification :

    recalcul à la main sur 16/20 : 
        0 jour → 16, 
        1 jour → 14,4, 
        3 jours → 11,2, 
        8 jours → 8
    Les 4 cas de l'énoncé sont bons.

### Échange 2 — 10:03 26/11
Demande :

    Utilise penalizedGrade dans SubmissionController, 
    GradeReport et CsvExport, pour que correction, 
    relevé et export donnent la même note. 
    La méthode est déjà recopiée dans les 3 classes
    Pas de nouvelle classe, ne touche à rien d'autre, 
    montre les changements fichier par fichier

Proposition : 
- `GradeReport` : le calcul de la note est remplacé par un appel à `penalizedGrade`.
- `CsvExport` : le calcul en `double` avec `Math.floor` est remplacé par `penalizedGrade`, la
  virgule décimale est conservée.
- `SubmissionController` : `get()` et `grade()` utilisent `penalizedGrade`. Dans `grade()`,
  `deadline` et `submittedAt` sont calculés avant la note, donc l'email et le JSON affichent la
  même note pénalisée.

Décision : `gardé avec une correction`

Pourquoi :

    Dans get(), rawPoints était passé en BigDecimal, ce qui aurait changé le JSON
    (16.00 au lieu de 16.0). J'ai comparé avec le code d'origine, 
    où c'était un double, et j'ai remis raw.doubleValue() 
    pour ne pas changer le format de l'API

Vérification :

    git diff relu en entier : pas de nouvelle classe, rien d'autre modifié,
    Math.floor supprimé. 

    relevé (Tri à bulles) : Alice 16.0, Bob 14.4, Clara 11.2, David 8.0 ;
    export CSV : 16,0 / 14,4 / 11,2 / 8,0, identique au relevé ;
    Emma (SQL, 27,5/40) : 13,8 dans le relevé et dans le CSV, alors que l'ancien
    Math.floor aurait donné 13,7

    Correction (GET /submissions/1 à 4) : 16.0 / 14.4 / 11.2 / 8.0,
    rawPoints toujours à 16.0 (format du JSON inchangé).

    Correction avec email (PUT /submissions/7/grade, 8 points sur 10, 1 jour de retard) :
    grade = 14.4 dans la réponse, et l'email reçu dans Mailpit affiche 14.4/20.
    Pour une même soumission en retard, la correction, le relevé et l'export donnent la même note

## Bilan
- Fichiers modifiés
  - `SubmissionController.java` : `get()` et `grade()` utilisent la note pénalisée
  - `GradeReport.java` :  le relevé utilise la note pénalisée
  - `CsvExport.java` : l'export utilise la note pénalisée, plus de Math.floor

- Ce que l'IA a bien fait : méthode penalizedGrade correcte, remplacements limités au calcul
- Ce que j'ai corrigé : méthodes en private static, et rawPoints remis en double dans get()
- Ce que j'ai vérifié moi-même : calcul à la main, git diff, relevé, CSV, correction et email