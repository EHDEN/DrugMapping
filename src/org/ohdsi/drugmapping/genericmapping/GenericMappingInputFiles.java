package org.ohdsi.drugmapping.genericmapping;

import java.util.ArrayList;
import java.util.Arrays;

import org.ohdsi.drugmapping.FormConversion;
import org.ohdsi.drugmapping.IngredientNameTranslation;
import org.ohdsi.drugmapping.MappingInputDefinition;
import org.ohdsi.drugmapping.UnitConversion;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class GenericMappingInputFiles extends MappingInputDefinition {

	
	public GenericMappingInputFiles() {
		inputFiles = new ArrayList<FileDefinition>(
				Arrays.asList(
						new FileDefinition(
								"Generic Drugs File",
								new String[] {
										"This file should contain the genric drugs from the source database."
						  		},
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
								}
						),
						new FileDefinition(
								"Ingredient Name Translation File",
								new String[] {
										"This file should contain the translation from source ingredient",
										"names to English."
						  		},
								new FileColumnDefinition[] {
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
								true
						),
						new FileDefinition(
								"Unit Mapping File",
								new String[] {
										"This file should contain the mapping from source units to CDM units."
						  		},
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
								true
						),
						new FileDefinition(
								"Dose Form Mapping File",
								new String[] {
										"This file should contain the mapping from source dose forms to CDM dose forms."
						  		},
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
								true
						),
						new FileDefinition(
								"CAS File",
								new String[] {
										"This file should contain CAS numbers with their chemical name an synonyms."
						  		},
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"CASNumber",
												new String[] {
														"This is the CAS number of the substance."
												}
										),
										new FileColumnDefinition(
												"ChemicalName",
												new String[] {
														"This is the chemical name of the substance."
												}
										),
										new FileColumnDefinition(
												"Synonyms",
												new String[] {
														"This is a list of synonyms for the chemical name separated", 
														"by a '|' character."
												}
										)
								},
								false
						),
						new FileDefinition(
								"Manual CAS Mappings File",
								new String[] {
										"This file should contain mappings of CAS numbers to CDM Ingredient concepts",
										"that overrule the automatic CAS mapping."
						  		},
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
								false
						),
						new FileDefinition(
								"Manual Ingedient Mappings - RxNorm File",
								new String[] {
										"This file should contain manual mappings of source ingredients to.",
										"CDM Ingredient concepts."
						  		},
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
								false
						),
						new FileDefinition(
								"Manual Drug Mappings File",
								new String[] {
										"This file should contain manual mappings from source drugs to",
										"clinical drugs, clinical drug comps, or clinical drug forms",
										"that overrule the automated mapping."
						  		},
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
								false
						)
				)
			);
	}
}
