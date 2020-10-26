package org.ohdsi.drugmapping.genericmapping;

import java.util.ArrayList;
import java.util.Arrays;

import org.ohdsi.drugmapping.FormConversion;
import org.ohdsi.drugmapping.IngredientNameTranslation;
import org.ohdsi.drugmapping.UnitConversion;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.files.InputFileDefinition;

public class GenericMappingInputFiles extends InputFileDefinition {

	
	public GenericMappingInputFiles() {
		inputFiles = new ArrayList<FileDefinition>(
				Arrays.asList(
						new FileDefinition(
								"Generic Drugs File",
								new String[] {
										"This file should contain the genric drugs from the source database."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
														"This is the code of the source drug."
												}
										),
										new FileColumnDefinition(
												"SourceName",
												new String[] {
														"This is the full name of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceATCCode",
												new String[] {
														"This is the ATC-code of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceFormulation",
												new String[] {
														"This is the pharmaceutical form of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceCount",
												new String[] {
														"The number of occurrences of the source drug in the source database."
												}
										),
										new FileColumnDefinition(
												"IngredientCode",
												new String[] {
														"This is the local code of the ingredient of the drug."
												},
												false
										),
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
														"This is the name of the ingredient of the drug."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
														"This is the English name of the ingredient of the drug."
												},
												false
										),
										new FileColumnDefinition(
												"Dosage",
												new String[] {
														"This is the dosage in mg of the drug."
												}
										),
										new FileColumnDefinition(
												"DosageUnit",
												new String[] {
														"This is the mg unit of the drug."
												}
										),
										new FileColumnDefinition(
												"CASNumber",
												new String[] {
														"This is the CAS number of the",
														"ingredient in this combination."
												}
										)
								},
								true,
								true
						),
						new FileDefinition(
								"Ingredient Name Translation File",
								new String[] {
										"This file should contain the translation from source ingredient",
										"names to English to overrule the existing translation."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"IngredientCode",
												new String[] {
														"This is the code of the source ingredient."
												}
										),
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
														"This is the source ingredient name."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
														"This is the English ingredient name."
												}
										),
								},
								IngredientNameTranslation.getDefaultFileName(),
								"Comma",
								"\"",
								true,
								true
						),
						new FileDefinition(
								"Unit Mapping File",
								new String[] {
										"This file should contain the mapping from source units to CDM units."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceUnit",
												new String[] {
														"This is the source unit."
												}
										),
										new FileColumnDefinition(
												"DrugCount",
												new String[] {
														"This is the number of drugs the source unit is used in."
												}
										),
										new FileColumnDefinition(
												"RecordCount",
												new String[] {
														"This is the number of data records the unit is used in."
												}
										),
										new FileColumnDefinition(
												"Factor",
												new String[] {
														"Multiply the source value with this value to get the CDM value."
												}
										),
										new FileColumnDefinition(
												"TargetUnit",
												new String[] {
														"This is the unit in CDM concept code terms."
												}
										),
										new FileColumnDefinition(
												"Comment",
												new String[] {
														"This is a comment."
												}
										)
								},
								UnitConversion.getDefaultFileName(),
								"Comma",
								"\"",
								true,
								true
						),
						new FileDefinition(
								"Dose Form Mapping File",
								new String[] {
										"This file should contain the mapping from source dose forms to CDM dose forms."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"DoseForm",
												new String[] {
														"This is the source dose form."
												}
										),
										new FileColumnDefinition(
												"Priority",
												new String[] {
														"This is an integer specifying the order of several mappings",
														"for the same source dose form."
												}
										),
										new FileColumnDefinition(
												"ConceptId",
												new String[] {
														"This is the concept_id of the target CDM dose form."
												}
										),
										new FileColumnDefinition(
												"ConceptName",
												new String[] {
														"This is the concept_name of the target CDM dose form."
												}
										),
										new FileColumnDefinition(
												"Comment",
												new String[] {
														"This is a comment."
												}
										)
								},
								FormConversion.getDefaultFileName(),
								"Comma",
								"\"",
								true,
								true
						),
						new FileDefinition(
								"Manual CAS Mappings File",
								new String[] {
										"This file should contain mappings of CAS numbers to CDM Ingredient concepts",
										"that overrule the automatic CAS mapping."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"CASNumber",
												new String[] {
														"This is the CAS number of the substance."
												}
										),
										new FileColumnDefinition(
												"concept_id",
												new String[] {
														"This is the CDM concept id of the ingredient the CAS number", 
														"should be mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_name",
												new String[] {
														"This is the CDM concept name of the ingredient the CAS number", 
														"should be mapped to."
												}
										)
								},
								false,
								true
						),
						new FileDefinition(
								"Manual Ingedient Overrule Mappings File",
								new String[] {
										"This file should contain manual mappings of source ingredients to.",
										"CDM Ingredient concepts that are used to correct incorrect automatic",
										"mappings."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
														"This is the code of the source ingredient.",
														"When empty drugs starting with the SourceName are mapped."
												}
										),
										new FileColumnDefinition(
												"SourceName",
												new String[] {
														"This is the name of the ingredient in the native language."
												}
										),
										new FileColumnDefinition(
												"concept_id",
												new String[] {
														"This is the CDM concept id of the ingredient the ingredient", 
														"should be mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_name",
												new String[] {
														"This is the CDM concept name of the ingredient the ingredient", 
														"should be mapped to."
												}
										),
										new FileColumnDefinition(
												"Comment",
												new String[] {
														"Comment on the mapping."
												}
										)
								},
								false,
								true
						),
						new FileDefinition(
								"Manual Ingedient Fallback Mappings File",
								new String[] {
										"This file should contain manual mappings of source ingredients to.",
										"CDM Ingredient concepts that are used when no automatic mapping",
										"could be made."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
														"This is the code of the source ingredient.",
														"When empty drugs starting with the SourceName are mapped."
												}
										),
										new FileColumnDefinition(
												"SourceName",
												new String[] {
														"This is the name of the ingredient in the native language."
												}
										),
										new FileColumnDefinition(
												"concept_id",
												new String[] {
														"This is the CDM concept id of the ingredient the ingredient", 
														"should be mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_name",
												new String[] {
														"This is the CDM concept name of the ingredient the ingredient", 
														"should be mapped to."
												}
										),
										new FileColumnDefinition(
												"Comment",
												new String[] {
														"Comment on the mapping."
												}
										)
								},
								false,
								true
						),
						new FileDefinition(
								"Manual Drug Mappings File",
								new String[] {
										"This file should contain manual mappings from source drugs to",
										"clinical drugs, clinical drug comps, or clinical drug forms",
										"that overrule the automated mapping."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
														"This is the code of the source drug."
												}
										),
										new FileColumnDefinition(
												"concept_id",
												new String[] {
														"This is the CDM concept id of the clinical drug,",
														"clinical drug comp, or clinical drug form",
														"the source drug should be mapped to."
												}
										)
								},
								false,
								true
						),
						new FileDefinition(
								"Ingredient Mapping Log File",
								new String[] {
										"This file contains the log of the ingredient mapping."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"IngredientCode",
												new String[] {
													"This is the code of the source ingredient."
												}
										),
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
													"This is the name of the source ingredient."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
													"This is the English name of the source ingredient."
												}
										),
										new FileColumnDefinition(
												"CASNumber",
												new String[] {
													"This is the CAS number of the source ingredient."
												}
										),
										new FileColumnDefinition(
												"MatchString",
												new String[] {
													"This is the matching criterion used to map the source ingredient."
												}
										),
										new FileColumnDefinition(
												"SourceCount",
												new String[] {
													"This is the use count of the source drug."
												}
										),
										new FileColumnDefinition(
												"concept_id",
												new String[] {
														"This is the concept id of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_name",
												new String[] {
														"This is the concept name of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"domain_id",
												new String[] {
														"This is the domain id of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"vocabulary_id",
												new String[] {
														"This is the vocabulary id of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_class_id",
												new String[] {
														"This is the concept class id of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"standard_concept",
												new String[] {
														"This is the standard concept flag of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"concept_code",
												new String[] {
														"This is the concept code of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"valid_start_date",
												new String[] {
														"This is the valid start date of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"valid_end_date",
												new String[] {
														"This is the valid end date of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"invalid_reason",
												new String[] {
														"This is the invalid reason of the CDM concept the ingredient is mapped to."
												}
										),
										new FileColumnDefinition(
												"ATC",
												new String[] {
													"This is the ATC code of the ingredient."
												}
										)
								},
								false,
								false
						),
						new FileDefinition(
								"DrugMapping Mapping Log File",
								new String[] {
										"This file contains the log of the drug mapping."
						  		},
								FileDefinition.DELIMITED_FILE,
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"MappingStatus",
												new String[] {
														"Mapped, Unmapped, or ManualMapping"
												}
										),
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
													"This is the code of the source drug."
												}
										),
										new FileColumnDefinition(
												"SourceName",
												new String[] {
													"This is the name of the source drug."
												}
										),
										new FileColumnDefinition(
												"SourceATCCode",
												new String[] {
													"These are the ATC codes of the source drug separated by |."
												}
										),
										new FileColumnDefinition(
												"SourceFormulation",
												new String[] {
													"These are the formulations of the source drug separated by |."
												}
										),
										new FileColumnDefinition(
												"SourceCount",
												new String[] {
													"This is the use count of the source drug."
												}
										),
										new FileColumnDefinition(
												"IngredientCode",
												new String[] {
													"This is the code of an ingredient of the source drug."
												}
										),
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
													"This is the name of the ingredient of the source drug",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
													"This is the English name of the ingredient of the source drug",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"CASNumber",
												new String[] {
													"This is the CAS number of the ingredient of the source drug",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"SourceIngredientAmount",
												new String[] {
													"This is the amount of the ingredient in the source drug",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"SourceIngredentUnit",
												new String[] {
													"This is the unit of the amount of the ingredient in the source drug",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"StrengthMarginPercentage",
												new String[] {
													"This is the strength margin used for the mapping",
													"in case of an ingredient mapping."
												}
										),
										new FileColumnDefinition(
												"MappingType",
												new String[] {
													"This the type of the mapping: Clinical Drug, Clinical Drug Form, Clinical Drug Comp, or Ingredient."
												}
										),
										new FileColumnDefinition(
												"MappingResult",
												new String[] {
													"This is the mapping result for this step in the mapping process."
												}
										)
								},
								false,
								false
						)
				)
			);
	}
	
	
	public FileDefinition getInputFileDefinition(String inputFileName) {
		FileDefinition inputFileDefinition = null; 
		for (FileDefinition fileDefinition : inputFiles) {
			if (fileDefinition.getFileName().equals(inputFileName)) {
				inputFileDefinition = fileDefinition;
				break;
			}
		}
		return inputFileDefinition;
	}
}
