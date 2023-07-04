package com.example.todolist.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.todolist.entity.Todo;
import com.example.todolist.form.TodoQuery;

public interface TodoDao {
	// JPQL/Criteria APIによる動的クエリは検索メソッドを自作してリポジトリを使わない
	// リポジトリを使わずにデータベースにアクセスする処理はDAOに記述する
	// DAO自体はただのクラスで、クラスの役割をそう言い表しただけ。
	// DAO Data Access Object

	// JPQL による検索
	// 文法上の操作対象はエンティティオブジェクト
	// エンティティはテーブルに紐づいているので、テーブルの検索となる
	List<Todo> findByJPQL(TodoQuery todoQuery);

	// Criteria API による検索
	// メソッドやメタクラスを使いJQPLを作成することによりエラーが起きないようになっている
	Page<Todo> findByCriteria(TodoQuery todoQuery, Pageable pageable);
}
