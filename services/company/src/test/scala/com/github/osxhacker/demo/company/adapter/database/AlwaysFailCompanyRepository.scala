package com.github.osxhacker.demo.company.adapter.database

import cats.ApplicativeThrow

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.company.domain.Company
import com.github.osxhacker.demo.company.domain.repository.CompanyRepository


/**
 * The '''AlwaysFailCompanyRepository''' type fulfills the
 * [[com.github.osxhacker.demo.company.domain.repository.CompanyRepository]] by
 * raising an `error` within the context ''F[_]''.
 */
final case class AlwaysFailCompanyRepository[F[_]] (val error : Throwable)
	(implicit private val applicativeThrow : ApplicativeThrow[F])
	extends CompanyRepository[F]
{
	override def delete (instance : Company) : F[Boolean] = failure ()


	override def exists (id : Identifier[Company]) : F[Option[Version]] =
		failure ()


	override def find (id : Identifier[Company]) : F[Company] = failure ()


	override def findAll () : fs2.Stream[F, Company] =
		fs2.Stream.raiseError (error)


	override def save (intent : Intent[Company]) : F[Option[Company]] =
		failure ()


	private def failure[A] () = applicativeThrow.raiseError[A] (error)
}
