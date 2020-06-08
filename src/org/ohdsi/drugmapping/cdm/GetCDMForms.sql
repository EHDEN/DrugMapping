SELECT DISTINCT form.*
FROM @vocab.concept_relationship relationship
  LEFT OUTER JOIN @vocab.concept form
    ON relationship.concept_id_2 = form.concept_id
WHERE relationship.relationship_id = 'RxNorm has dose form'
ORDER BY concept_id