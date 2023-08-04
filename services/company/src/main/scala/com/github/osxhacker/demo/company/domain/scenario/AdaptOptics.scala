package com.github.osxhacker.demo.company.domain.scenario

import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined
import eu.timepit.refined.api.Refined
import monocle.Getter

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.company.domain.{
	Company,
	CompanyStatus
	}


/**
 * The '''AdaptOptics''' `object` defines the algorithms for defining
 * [[cats.data.Kleisli]]s based on [[monocle]] optics types for use in
 * interacting with ''SourceT'' instances to provide properties relevant to
 * [[com.github.osxhacker.demo.company.domain.Company]].
 */
object AdaptOptics
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.validated._
	import mouse.any._
	import refined.cats.syntax._


	/// Class Types
	type F[+A] = ValidatedNec[String, A]
	type KleisliType[SourceT, A] = Kleisli[F, SourceT, A]


	@inline
	def apply[SourceT, ValueT] (lens : Getter[SourceT, ValueT])
		: KleisliType[SourceT, ValueT] =
		Kleisli[F, SourceT, ValueT] {
			lens.get (_)
				.validNec
			}


	@inline
	def id[SourceT, CandidateT] (lens : Getter[SourceT, CandidateT])
		(implicit parser : Identifier.Parser[Company, CandidateT])
		: KleisliType[SourceT, Identifier[Company]] =
		Kleisli[F, SourceT, Identifier[Company]] {
			candidate =>
				parser (lens.get (candidate)).toEither
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	@inline
	def name[SourceT, P] (lens : Getter[SourceT, Refined[String, P]])
		: KleisliType[SourceT, Company.Name] =
		Kleisli[F, SourceT, Company.Name] {
			candidate =>
				lens.get (candidate).value |> Company.Name.validateNec
			}


	@inline
	def slug[SourceT, P] (lens : Getter[SourceT, Refined[String, P]])
		: KleisliType[SourceT, Slug] =
		Kleisli[F, SourceT, Slug] {
			candidate =>
				Slug[ErrorOr] (lens.get (candidate).value)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	@inline
	def status[SourceT, StatusT <: AnyRef] (lens : Getter[SourceT, StatusT])
		: KleisliType[SourceT, CompanyStatus] =
		Kleisli[F, SourceT, CompanyStatus] {
			candidate =>
				CompanyStatus
					.withNameInsensitiveEither (lens.get (candidate).toString)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	@inline
	def version[SourceT, P] (lens : Getter[SourceT, Refined[Int, P]])
		: KleisliType[SourceT, Version] =
		Kleisli {
			source =>
				Version[ErrorOr] (lens.get (source).value)
					.leftMap (_.getMessage)
					.toValidatedNec
			}
}

