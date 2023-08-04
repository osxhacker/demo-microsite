package com.github.osxhacker.demo.chassis.domain.repository

import cats._


/**
 * The '''Intent''' type defines the contract for a workflow to be able to
 * declare what type of `save` operation a workflow wants a
 * [[com.github.osxhacker.demo.chassis.domain.repository.Repository]] to
 * perform.
 */
sealed trait Intent[+A]
{
	/**
	 * The filter method returns '''this''' if it is not ignored ''and'' the
	 * given '''predicate''' returns `true` for the contained ''A''.
	 */
	def filter (predicate : A => Boolean) : Intent[A]


	/**
	 * The filterNot method returns '''this''' if it is not ignored ''and'' the
	 * given '''predicate''' returns `false` for the contained ''A''.
	 */
	final def filterNot (predicate : A => Boolean) : Intent[A] =
		filter (!predicate (_))


	/**
	 * The flatMap method returns the result of applying '''f''' to '''this'''
	 * if it is not ignored.
	 */
	def flatMap[B] (f : A => Intent[B]) : Intent[B]


	/**
	 * The fold method defines a catamorphism which will will produce the
	 * '''ignore''' value when '''this''' is an
	 * [[com.github.osxhacker.demo.chassis.domain.repository.Ignore]] and the
	 * '''executable''' ''B'' value when it is not.
	 */
	def fold[B] (ignore : => B, executable : A => B) : B


	/**
	 * The map method applies '''f''' to '''this''' if it is not ignored.
	 */
	def map[B] (f : A => B) : Intent[B]


	/**
	 * The mapFilter method combines the `map` and `filter` operations by using
	 * the returned ''Option''.
	 */
	def mapFilter[B] (f : A => Option[B]) : Intent[B]
}


object Intent
{
	/// Implicit Conversions
	implicit val intentFunctor : Functor[Intent] = new Functor[Intent] {
		override def map[A, B] (fa : Intent[A])
			(f : A => B)
			: Intent[B] =
			fa map f
		}

	implicit val intentFunctorFilter : FunctorFilter[Intent] =
		new FunctorFilter[Intent] {
			override val functor : Functor[Intent] = intentFunctor


			override def filter[A] (fa : Intent[A])
				(f : A => Boolean)
				: Intent[A] =
				fa filter f


			override def filterNot[A] (fa : Intent[A])
				(f : A => Boolean)
				: Intent[A] =
				fa filterNot f


			override def mapFilter[A, B] (fa : Intent[A])
				(f : A => Option[B])
				: Intent[B] =
				fa mapFilter f
			}

	implicit val intentTraverse : Traverse[Intent] =
		new Traverse[Intent] {
			override def traverse[G[_], A, B] (fa : Intent[A])
				(f : A => G[B])
				(implicit applicative : Applicative[G])
				: G[Intent[B]] =
				fa.fold (
					applicative.pure (Ignore),
					value => applicative.map (f (value)) (b => fa.map (_ => b))
					)


			override def foldLeft[A, B] (fa : Intent[A], b : B)
				(f : (B, A) => B)
				: B =
				fa.fold (b, f (b, _))


			override def foldRight[A, B] (fa : Intent[A], lb : Eval[B])
				(f : (A, Eval[B]) => Eval[B])
				: Eval[B] =
				fa.fold (lb, f (_, lb))
			}
}


sealed trait ExecutableIntent[A]
	extends Intent[A]
{
	/// Class Imports
	import mouse.boolean._


	/// Instance Properties
	val value : A


	final override def filter (predicate : A => Boolean) : Intent[A] =
		predicate (value).fold (this, Ignore)


	final override def flatMap[B] (f : A => Intent[B]) : Intent[B] = f (value)


	final override def fold[B] (ignore : => B, executable : A => B) : B =
		executable (value)


	final override def mapFilter[B] (f : A => Option[B]) : Intent[B] =
		f (value).fold[Intent[B]] (Ignore) (b => map (_ => b))
}


final case class CreateIntent[A] (override val value : A)
	extends ExecutableIntent[A]
{
	override def map[B] (f : A => B) : CreateIntent[B] = CreateIntent (f (value))
}


object CreateIntent
{
	/// Implicit Conversions
	implicit val createApplicative : Applicative[CreateIntent] =
		new Applicative[CreateIntent] {
			override val unit : CreateIntent[Unit] = CreateIntent ({})


			override def ap[A, B] (fab : CreateIntent[A => B])
				(fa : CreateIntent[A])
				: CreateIntent[B] =
				fa map fab.value


			override def map[A, B] (fa : CreateIntent[A])
				(f : A => B)
				: CreateIntent[B] =
				fa map f


			override def pure[A] (a : A) : CreateIntent[A] = apply (a)


			override def void[A] (fa : CreateIntent[A])
				: CreateIntent[Unit] =
				unit
			}
}


case object Ignore
	extends Intent[Nothing]
{
	override def filter (predicate : Nothing => Boolean) : Intent[Nothing] =
		this


	override def flatMap[B] (f : Nothing => Intent[B]) : Intent[B] = this


	override def fold[B] (ignore : => B, executable : Nothing => B): B = ignore


	override def map[B] (f : Nothing => B) : Intent[B] = this


	override def mapFilter[B] (f : Nothing => Option[B]) : Intent[B] = this
}


final case class UpdateIntent[A] (override val value : A)
	extends ExecutableIntent[A]
{
	override def map[B] (f : A => B) : UpdateIntent[B] = UpdateIntent (f (value))
}


object UpdateIntent
{
	/// Implicit Conversions
	implicit val updateApplicative : Applicative[UpdateIntent] =
		new Applicative[UpdateIntent] {
			override val unit : UpdateIntent[Unit] = UpdateIntent ({})


			override def ap[A, B] (fab : UpdateIntent[A => B])
				(fa : UpdateIntent[A])
				: UpdateIntent[B] =
				fa map fab.value


			override def map[A, B] (fa : UpdateIntent[A])
				(f : A => B)
				: UpdateIntent[B] =
				fa map f


			override def pure[A] (a : A) : UpdateIntent[A] = apply (a)


			override def void[A] (fa : UpdateIntent[A])
				: UpdateIntent[Unit] =
				unit
			}
}


final case class UpsertIntent[A] (override val value : A)
	extends ExecutableIntent[A]
{
	override def map[B] (f : A => B) : UpsertIntent[B] = UpsertIntent (f (value))
}


object UpsertIntent
{
	/// Implicit Conversions
	implicit val upsertApplicative : Applicative[UpsertIntent] =
		new Applicative[UpsertIntent] {
			override val unit : UpsertIntent[Unit] = UpsertIntent ({})


			override def ap[A, B] (fab : UpsertIntent[A => B])
				(fa : UpsertIntent[A])
				: UpsertIntent[B] =
				fa map fab.value


			override def map[A, B] (fa : UpsertIntent[A])
				(f : A => B)
				: UpsertIntent[B] =
				fa map f


			override def pure[A] (a : A) : UpsertIntent[A] = apply (a)


			override def void[A] (fa : UpsertIntent[A])
				: UpsertIntent[Unit] =
				unit
			}
}

