package com.jan.food.fixtures

import com.jan.food.data.dataSource.food.CheckResponse
import com.jan.food.data.dataSource.food.CheckSubject
import com.jan.food.data.dataSource.food.RestrictionResult

/**
 * Builds a sample `/v1/check` [CheckResponse] DTO. Every field has a sensible default so a test
 * overrides only what it asserts on.
 */
fun checkResponse(
    barcode: String = "5901234123457",
    name: String? = "Test Cookies",
    source: String = "usda",
    found: Boolean = true,
    results: List<RestrictionResult> =
        listOf(
            RestrictionResult(restriction = "gluten", status = "contains"),
        ),
    type: String = "product",
): CheckResponse =
    CheckResponse(
        subject = CheckSubject(type = type, barcode = barcode, name = name),
        source = source,
        found = found,
        results = results,
    )
