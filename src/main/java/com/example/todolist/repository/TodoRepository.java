package com.example.todolist.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.todolist.entity.Todo;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Integer> {
	// SpringBootにはエンティティに対する処理を自動生成する仕組みがある
	// それを実現するのがリポジトリ

	// JpaRepository
	// 第一引数 リポジトリが対象とするエンティティ
	// 第二引数 対象エンティティで＠Idが指定されているプロパティのクラス

	// このクラスには抽象メソッドがないが、CRUDにまつわる処理が自動生成される

	List<Todo> findByTitleLike(String title);

	List<Todo> findByImportance(Integer importance);

	List<Todo> findByUrgency(Integer urgency);

	List<Todo> findByDeadlineBetweenOrderByDeadlineAsc(Date from, Date to);

	List<Todo> findByDeadlineGreaterThanEqualOrderByDeadlineAsc(Date from);

	List<Todo> findByDeadlineLessThanEqualOrderByDeadlineAsc(Date to);

	List<Todo> findByDone(String done);
}
