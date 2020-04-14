package org.ohdsi.drugmapping.genericmapping;

import java.util.ArrayList;
import java.util.Arrays;
import org.ohdsi.drugmapping.MappingInputDefinition;
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
												"IngredientName",
												new String[] {
														"This is the name of the ingredient of the drug."
												}
										),
										new FileColumnDefinition(
												"IngredientCode",
												new String[] {
														"This is the local code of the ingredient of the drug.",
														"This column is optional."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
														"This is the English name of the ingredient of the drug."
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
								"CAS File",
								new String[] {
										"This file should contain CAS numbers with their chemical name an synonyms.",
										"This file is optional."
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
														"This is a list of synonyms for the chemicl name separated by a '|' character."
												}
										)
								}
						),
						new FileDefinition(
								"Manual Mappings File",
								new String[] {
										"This file should contain manual mappings from source drugs to",
										"clinical drugs, clinical drug comps, or clinical drug forms",
										"that overrule the automated mapping.",
										"This file is optional."
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
								}
						)
				)
			);
	}
}
