package com.github.osxhacker.demo.chassis.domain.repository

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}


/**
 * The '''Repository''' type defines the common contract for __all__ Domain
 * Object Model repositories.  ''EntityT''-specific implementations are expected
 * to add additional semantically meaningful behaviour.
 */
trait Repository[F[_], EntityT]
{
	/**
	 * The delete method attempts to alter the persistent store such that the
	 * given '''instance''' no longer participates in '''Repository'''
	 * operations.  This can be by erasure or a "soft delete" as applicable.
	 */
	def delete (instance : EntityT) : F[Boolean]


	/**
	 * The exists method determines if an ''EntityT'' has been persisted as
	 * identified by the given '''id'''.  If so, the current
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] is returned.
	 */
	def exists (id : Identifier[EntityT]) : F[Option[Version]]


	/**
	 * The find method attempts to retrieve an ''EntityT'' by a unique
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]], raising
	 * an error in ''F'' if it fails.
	 */
	def find (id : Identifier[EntityT]) : F[EntityT]


	/**
	 * The findAll method attempts to retrieve all ''EntityT''s known to the
	 * persistent store.
	 */
	def findAll () : fs2.Stream[F, EntityT]


	/**
	 * The queryBy method provides the ability to retrieve ''EntityT'' types
	 * based on an arbitrary ''EntityT''
	 * [[com.github.osxhacker.demo.chassis.domain.Specification]].  A production
	 * implementation would employ an interpreter to optimize the `filter` based
	 * on what the underlying concrete persistent store supports.
	 *
	 * Since this is a demonstration project, a simplistic approach is taken
	 * here.
	 *
	 * @see [[https://github.com/osxhacker/scala-bson-query Scala BSON Query]]
	 */
	final def queryBy (specification : Specification[EntityT])
		: fs2.Stream[F, EntityT] =
		findAll ().filter (specification.isSatisfiedBy)


	/**
	 * The save method attempts to persist an ''EntityT'' as expressed by the
	 * '''intent''' given.  __How__ each
	 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] is
	 * interpreted is an implementation detail so long as the ''spirit'' of
	 * each is upheld.  If the '''intent''' is to
	 * [[com.github.osxhacker.demo.chassis.domain.repository.Ignore]], then
	 * ''None'' is `lift`ed into ''F''.
	 *
	 * Situations where the ''EntityT'' cannot be found to manipulate, such as
	 * version conflicts, __must__ be expressed as errors within ''F''.
	 */
	def save (intent : Intent[EntityT]) : F[Option[EntityT]]
}

