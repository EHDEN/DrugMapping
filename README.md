Introduction
========

ADD Schema drawing and explanation

Features
========

Screenshots
===========

Technology
============
DrugMapping is a pure Java application that uses JDBC to connect to the respective databases containing the standardized vocabulary

Getting Started
===============
The tool runs against an input files that contains the following content:

Column Name	| Contents
 --- | ---
SourceCode |	The code of the source drug.
SourceName |	The original name of the source drug.
SourceATCCode	| The ATC code if available.
SourceFormulation |	The formulation of the source drug.
SourceCount	| The number of records in the database containing the source drug.
IngredientName	| The original name of an active ingredient of the source drug.
IngredientNameEnglish |	The English name of the ingredient.
Dosage	| The dosage of the ingredient in the source drug.
DosageUnit |	The unit of the dosage of the ingredient in the source drug.
CASNumber	| The CAS-number of the ingredient if available.

TO FINISH

Getting Involved
=============
* We use the [GitHub issue tracker](../../issues) for all bugs/issues/enhancements/questions
* Historically, all files have CRLF line endings. Please configure your IDE and local git to keep line endings as is. This avoids merge conflicts.

License
=======
DrugMapping is licensed under Apache License 2.0

### Development status

Beta. This program is being tested.
