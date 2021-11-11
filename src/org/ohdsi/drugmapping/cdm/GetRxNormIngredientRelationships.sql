SELECT relationship.relationship_id,
       drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       drug.vocabulary_id AS drug_vocabulary_id,
       drug.concept_class_id AS drug_concept_class_id,
       drug.concept_code AS drug_concept_code,
       ingredient.concept_id AS ingredient_concept_id,
       synonym.concept_synonym_name AS drug_synonym_name
FROM @vocab.concept_relationship relationship
  LEFT OUTER JOIN @vocab.concept drug
    ON relationship.concept_id_1 = drug.concept_id
  LEFT OUTER JOIN @vocab.concept ingredient
    ON relationship.concept_id_2 = ingredient.concept_id
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON drug.concept_id = synonym.concept_id
WHERE drug.concept_id <> ingredient.concept_id
AND   (
		(relationship.relationship_id = 'Maps to') OR
		(relationship.relationship_id = 'Form of') OR
		(relationship.relationship_id = 'ATC - RxNorm') OR
		(relationship.relationship_id = 'Concept replaced by') OR
		(relationship.relationship_id LIKE '%RxNorm eq')
	  )
AND   (
		(drug.concept_class_id = 'Ingredient') OR
		(drug.concept_class_id = 'Precise Ingredient') OR
		(drug.concept_class_id = 'Substance') OR
		(drug.concept_class_id = 'ATC 5th') OR
		(drug.concept_class_id = 'ATC 4th') OR
		(drug.concept_class_id = 'Pharma/Biol Product') OR
		(drug.concept_class_id = '11-digit NDC') OR
		(drug.concept_class_id = '9-digit NDC')
      )
AND   ingredient.concept_class_id LIKE '%ingredient'
AND   ingredient.standard_concept = 'S'
ORDER BY relationship.relationship_id,
         drug.concept_id,
         synonym.concept_synonym_name,
         ingredient.concept_id