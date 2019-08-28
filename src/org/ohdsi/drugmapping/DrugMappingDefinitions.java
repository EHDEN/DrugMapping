package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class DrugMappingDefinitions {
	
	public static final List<FileDefinition> FILES = new ArrayList<FileDefinition>(
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
													"This is the GPK-code of the drug."
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
									),
									new FileColumnDefinition(
											"ConceptId",
											new String[] {
													"This is the CDM concept_id of the ingredient."
											}
									),
									new FileColumnDefinition(
											"DomainId",
											new String[] {
													"This is the CDM domain_id of the ingredient."
											}
									),
									new FileColumnDefinition(
											"ConceptName",
											new String[] {
													"This is the CDM concept_name of the ingredient."
											}
									),
									new FileColumnDefinition(
											"Comment",
											new String[] {
													"This is the USAGI comment on the ingredient."
											}
									),
									new FileColumnDefinition(
											"ConceptClassId",
											new String[] {
													"This is the CDM concept_class_id of the ingredient."
											}
									),
									new FileColumnDefinition(
											"VocabularyId",
											new String[] {
													"This is the CDM vocabulary_id of the ingredient."
											}
									),
									new FileColumnDefinition(
											"ConceptCode",
											new String[] {
													"This is the CDM concept_code of the ingredient."
											}
									),
									new FileColumnDefinition(
											"ValidStartDate",
											new String[] {
													"This is the CDM valid_start_date of the ingredient."
											}
									),
									new FileColumnDefinition(
											"ValidEndDate",
											new String[] {
													"This is the CDM valid_end_date of the ingredient."
											}
									),
									new FileColumnDefinition(
											"InvalidReason",
											new String[] {
													"This is the CDM invalid_reason of the ingredient."
											}
									),
									new FileColumnDefinition(
											"Info",
											new String[] {
													"This is the USAGI ADDITIONALINFORMATION on the ingredient."
											}
									)
							}
					)/*,
					new FileDefinition(
							"Generic Drugs File",
							new String[] {
									"This file should contain the genric drugs from the source database."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"SourceCode",
											new String[] {
													"This is the GPK-code of the drug."
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
							"Manual Translation File",
							new String[] {
									"This file should contain corrections on translations in the Genric Drugs File.",
									"This file is optional."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"IngredientName",
											new String[] {
													"This is the name of an ingredient."
											}
									),
									new FileColumnDefinition(
											"IngredientNameEnglish",
											new String[] {
													"This is the English name of the ingredient."
											}
									)
							}
					)
					*/
			)
		);

}
