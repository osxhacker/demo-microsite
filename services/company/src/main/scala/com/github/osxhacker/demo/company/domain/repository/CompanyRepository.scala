package com.github.osxhacker.demo.company.domain.repository

import com.github.osxhacker.demo.chassis.domain.repository.Repository
import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''CompanyRepository''' type defines the
 * [[com.github.osxhacker.demo.chassis.domain.repository.Repository]]
 * contract for managing the persistent store representation of
 * [[com.github.osxhacker.demo.company.domain.Company]] instances.
 */
trait CompanyRepository[F[_]]
	extends Repository[F, Company]

