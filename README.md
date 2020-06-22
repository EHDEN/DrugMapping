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

**Required Input files**

_Generic Drugs File_

| **Column Name** | **Contents** |
| --- | --- |
| SourceCode | The code of the source drug. |
| SourceName | The original name of the source drug. |
| SourceATCCode | The ATC code if available. |
| SourceFormulation | The formulation of the source drug. |
| SourceCount | The number of records in the database containing the source drug. |
| IngredientName | The original name of an active ingredient of the source drug. |
| Dosage | The dosage of the ingredient in the source drug. |
| DosageUnit | The unit of the dosage of the ingredient in the source drug. |
| CASNumber | The CAS-number of the ingredient if available. |

When a source drug has several active ingredients this file should contain a record for each ingredient, repeating the SourceCode, SourceName, SourceATCCode, SourceFormulation, and SourceCount values.

The DosageUnit may also contain units like mg/ml.

**How to use.**

Make sure you have a database containing the CDM vocabularies. The DrugMapping tool supports PostgreSQL, Oracle, and SQL Server. **Only PostgreSQL is tested.**

The DrugMapping tool consists of two files released in a .zip file:

DrugMapping &lt;version&gt;.dat

DrugMapping \&lt;version\&gt;.jar

Extract the zip file.

Start the the DrugMapping tool by double clicking the .jar file.

First you have to define the CDM database connection. To do this click on the &quot;Select&quot; button in the &quot;CDM Database&quot; box. Once defined, you can save this definition to a file using the menu option File-Save Database Settings. In another run you can load these settings again with the menu option File-Load Database Settings.

Second you have to specify the generic drugs file and how the columns of the file map to the description of the file shown above. The other files you can specify are not required and will be described later. As with the database settings you can save the file settings using the File-Save File Settings. In another run you can load these settings again with the menu option File-Load File Settings.

The default General Settings will work well but in case you want to change them you can also save them using the menu option File-Save General Settings. In another run you can load these settings again with the menu option File-Load General Settings.

At first run it will collect ingredient names, units and formulations from the source drug file and then it stops after creating a translation file for the ingredient names and empty mapping files for the units and formulations.

_Ingredient Name Translation File_

| **Column Name** | **Contents** |
| --- | --- |
| IngredientName | The source ingredient name |
| IngredientNameEnglish | The source ingredient name translated to English. |

_Unit Mapping File_

| **Column Name** | **Contents** |
| --- | --- |
| SourceUnit | The source unit (combination). |
| DrugCount | Number of drugs the unit is used in. |
| RecordCount | The number of data records the unit is used in. |
| Factor | The multiplication factor where SourceUnit \* Factor = TargeUnit. |
| TargetUnit | The target unit (combination) from CDM in terms of concept\_code. |
| Comment | Comments |

_Dose Form Mapping File_

| **Column Name** | **Contents** |
| --- | --- |
| DoseForm | The source dose form |
| Priority | Any whole number specifying the order the dose forms are used to map the drugs. Lower number comes first. |
| ConceptId | The concept\_id of the CDM dose form the source dose form is mapped to. |
| ConceptName | The concept\_name of the CDM dose form the source dose form is mapped to. |
| Comments | Comments |

After you have specified the initial ingredient name translations and unit and formulation mappings you are ready to perform the real mapping. The mapping will create several files all starting with the date of today (yyyy-mm-dd) and a two digit sequence number. The most important files I will describe here.

_\&lt;yyyy-mm-dd ss\&gt; DrugMapping Log.txt_

This is the log file containing console output of the DrugMapping tool.

_\&lt;yyyy-mm-dd ss\&gt; DrugMapping IngredientMapping Results.csv_

This file contains information on how source ingredients are mapped to RxNorm (Extension) ingredients. It contains the following columns:

| **Column** | **Content** |
| --- | --- |
| IngredientCode | The code of the source ingredient. |
| IngredientName | The original name of the source ingredient. |
| IngredientNameEnglish | The English name of the source ingredient. |
| CASNumber | The CAS-number of the ingredient if available. |
| MatchString | The way the source ingredient is matched with the RxNorm (Extension) ingredient. |
| SourceCount | The number of records in the database containing the source ingredient. |
| concept\_id | The concept\_id of the RxNorm (Extension) ingredient. |
| concept\_name | The concept\_name of RxNorm (Extension) ingredient. |
| domain\_id | The domain\_id of RxNorm (Extension) ingredient. |
| vocabulary\_id | The vocabulary\_id of RxNorm (Extension) ingredient. |
| concept\_class\_id | The concept\_class of RxNorm (Extension) ingredient. |
| standard\_concept | The standard\_concept of RxNorm (Extension) ingredient. |
| concept\_code | The concept\_code of RxNorm (Extension) ingredient. |
| valid\_start\_date | The valid\_start\_date of RxNorm (Extension) ingredient. |
| valid\_end\_date | The valid\_end\_date of RxNorm (Extension) ingredient. |
| invalid\_reason | The invalid\_reason of RxNorm (Extension) ingredient. |
| ATC | The ATC code of the source ingredient if available. |

The file sorted descending on the SourceCount column.

_\&lt;yyyy-mm-dd ss\&gt; DrugMapping Mapping Results.csv_

This file contains information on how the source drugs are mapped to the RxNorm (Extension) drugs. It contains the following columns:

| **Column** | **Content** |
| --- | --- |
| MappingStatus | &quot;Mapped&quot; or &quot;Unmapped&quot; |
| SourceCode | The code of the source drug. |
| SourceName | The original name of the source drug. |
| SourceATCCode | The ATC code if available. |
| SourceFormulation | The formulation of the source drug. |
| SourceCount | The number of records in the database containing the source drug. |
| _The following columns are filled with a \* when the source drug is mapped or with the specified content when the source drug could not be mapped but its ingredients could be mapped. In that case the file contains a line for each of the ingredients._ |
| IngredientCode | The code of the source ingredient |
| IngredientName | The original name of an active ingredient of the source drug. |
| IngredientNameEnglish | The English name of the ingredient. |
| CASNumber | The CAS-number of the ingredient. |
| SourceIngredientUnit | The source unit of the source ingredient. |
| SourceIngredientAmount | The source amount of the source ingredient. |
| The following two columns contain information about the mapping steps. |
| MappingType | The mapping that is attempted. |
| MappingResult | The (intermediate) result of the attempted mapping. |
| Concept1 | This column and the following columns contain concepts that are involved in this mapping result.|
| ''' ||
| ConceptN | |

The file is sorted descending on the SourceCount column.

**Additional input files**

_CAS File_

This file should contain CAS numbers with their chemical name an synonyms.

This file is optional.

| **Column** | **Content** |
| --- | --- |
| CASNumber | This is the CAS number of the substance. |
| ChemicalName | This is the chemical name of the substance. |
| Synonyms | This is a list of synonyms for the chemical name separated by a &#39;|&#39; character. |

_Manual CAS Mappings File_

This file should contain mappings of CAS numbers to CDM Ingredient concepts that overrule the automatic CAS mapping.

This file is optional.

| **Column** | **Content** |
| --- | --- |
| CASNumber | This is the CAS number of the substance. |
| concept\_id | This is the CDM concept id of the ingredient the CAS number should be mapped to. |
| concept\_name | This is the CDM concept name of the ingredient the CAS number should be mapped to. |

_Manual Ingedient Mappings - RxNorm File_

This file should contain manual mappings of source ingredients to CDM Ingredient concepts.

This file is optional.

| **Column** | **Content** |
| --- | --- |
| SourceCode | This is the code of the source ingredient. When empty drugs starting with the SourceName are mapped. |
| SourceName | This is the name of the ingredient in the native language. |
| concept\_id | This is the CDM concept id of the ingredient the ingredient should be mapped to. |
| concept\_name | This is the CDM concept name of the ingredient the ingredient should be mapped to. |
| Comment | Comment on the mapping. |

_Manual Drug Mappings File_

This file should contain manual mappings from source drugs to clinical drugs, clinical drug comps, or clinical drug forms that overrule the automated mapping.

This file is optional.

| **Column** | **Content** |
| --- | --- |
| SourceCode | This is the code of the source drug. |
| concept\_id | This is the CDM concept id of the clinical drug, clinical drug comp, or clinical drug form the source drug should be mapped to. |

**Command line options**

The DrugMapping tool can also be started from the command line with the command:

java -jar \&lt;DrugMapping .jar file\&gt; [databasesettings=\&lt;database settings file\&gt; password=\&lt;database password\&gt;] [filesettings=\&lt;file settings file\&gt;] [generalsettings=\&lt;general settings file\&gt;] [path=\&lt;path where result files are written\&gt;] [autostart=yes]

[…] means the option is optional. The square brackets should not be written in the command.

Getting Involved
=============
* We use the [GitHub issue tracker](../../issues) for all bugs/issues/enhancements/questions
* Historically, all files have CRLF line endings. Please configure your IDE and local git to keep line endings as is. This avoids merge conflicts.

License
=======
DrugMapping is licensed under Apache License 2.0

### Development status

Beta. This program is being tested.
