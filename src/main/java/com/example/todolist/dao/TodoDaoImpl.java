package com.example.todolist.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.todolist.common.Utils;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.Todo_;
import com.example.todolist.form.TodoQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TodoDaoImpl implements TodoDao {
	private final EntityManager entityManager;

	@Override
	public List<Todo> findByJPQL(TodoQuery todoQuery) {
		// 1. JPQLを文字列として組み立てる
		// 2. 検索条件値(パラメータ)を保存
		// 3. 1.の結果からQueryオブジェクトを生成する
		// 4. 2.のパラメータをQueryオブジェクトにセットする
		// 5. Queryオブジェクトから検索結果を取得する

		StringBuilder sb = new StringBuilder("select t from Todo t where 1 = 1");
		// from句のTodoはTodoエンティティを示す。todoテーブルではない。
		// Todoエンティティは@Tableでtodoテーブルと関連づけられているので、最終的にはテーブルへの検索となる
		// tはエイリアス。select句に書くとTodoの全プロパティを取得する
		List<Object> params = new ArrayList<>();
		// このarraylistにパラメータを保存する。格納した順番でプレースホルダを置換する。
		int pos = 0;

		// 実行する JPQL の組み立て
		// 件名
		if (todoQuery.getTitle().length() > 0) {
			sb.append(" and t.title like ?" + (++pos));
			params.add("%" + todoQuery.getTitle() + "%");
		}
		// 重要度
		if (todoQuery.getImportance() != -1) {
			sb.append(" and t.importance = ?" + (++pos)); // ①
			params.add(todoQuery.getImportance()); // ②
		}
		// 緊急度
		if (todoQuery.getUrgency() != -1) {
			sb.append(" and t.urgency = ?" + (++pos)); // ①
			params.add(todoQuery.getUrgency()); // ②
		}
		// 期限：開始～
		if (!todoQuery.getDeadlineFrom().equals("")) {
			sb.append(" and t.deadline >= ?" + (++pos)); // ①
			params.add(Utils.str2date(todoQuery.getDeadlineFrom())); // ②
		}
		// ～期限：終了で検索
		if (!todoQuery.getDeadlineTo().equals("")) {
			sb.append(" and t.deadline <= ?" + (++pos)); // ①
			params.add(Utils.str2date(todoQuery.getDeadlineTo())); // ②
		}
		// 完了
		if (todoQuery.getDone() != null && todoQuery.getDone().equals("Y")) {
			sb.append(" and t.done = ?" + (++pos)); // ①
			params.add(todoQuery.getDone()); // ②
		}
		// order
		sb.append(" order by id");
		Query query = entityManager.createQuery(sb.toString()); // ③
		for (int i = 0; i < params.size(); ++i) { // ④
			query = query.setParameter(i + 1, params.get(i));
		}
		@SuppressWarnings("unchecked")
		List<Todo> list = query.getResultList(); // ⑤
		return list;

	}

	// Criteria API による検索
	@Override
	public Page<Todo> findByCriteria(TodoQuery todoQuery, Pageable pageable) {
		// CriteriaBuilderインターフェース
		// Criteria APIによる検索を管理する
		//
		// CriteriaQueryインターフェース
		// JPQLのselect,from,whereに相当するメソッドを設定し、クエリを生成する
		//
		// Rootインターフェース
		// エンティティの列に関する情報を表す

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<Todo> query = builder.createQuery(Todo.class);
		// 引数には検索で取得するエンティティクラスのClassインスタンスを渡す
		// クラスインスタンスは、そのクラスで定義されているフィールドやメソッドの情報を保持

		Root<Todo> root = query.from(Todo.class);
		// rootの生成
		// rootは検索対象がTodoの全プロパティであることを示す

		// 例)
		// List<Todo> list =
		// entityManager.createQuery(query.select(root)).getResultList();
		// query.select()は取得するプロパティを指定する。queryは検索対象エンティティがTodoである
		// rootはTodoの全プロパティを示す。よってquery.select(root)はtodoテーブルの前列を取得対象とする
		// createQuery()は引数にしたがってtypedQuery型のクエリを作成する
		// ここではwhereに相当する条件がないので、todoテーブルの全件を取得するクエリとなる
		// getResultList()でクエリを実行する。

		// 例）
		// query = query.select(root).where(builder.equal(root.get("importance"),1);
		// これは select * from todo where importance=1 を示す
		// equalは第一引数と第二引数が等しいことを示すpredicate型を返す

		// 例) Andで複合条件にする
		// query = query.select(root).where(
		// builder.equal(root.get("importance"),1),
		// builder.and(builder.equal(root.get("urgency"),1))
		// );
		// select * from todo where importance=1 and urgency=1 を示す

		List<Predicate> predicates = new ArrayList<>();
		// 前述のwhereに条件を羅列する方式だと煩雑なため、
		// listにクエリを格納し、最後にpredicateの配列にしてqueryの引数に渡す。

		// 件名
		String title = "";
		if (todoQuery.getTitle().length() > 0) {
			title = "%" + todoQuery.getTitle() + "%";
		} else {
			title = "%";
		}
		predicates.add(builder.like(root.get(Todo_.TITLE), title));
		// 重要度
		if (todoQuery.getImportance() != -1) {
			predicates.add(builder.and(builder.equal(root.get(Todo_.IMPORTANCE), todoQuery.getImportance())));
		}
		// 緊急度
		if (todoQuery.getUrgency() != -1) {
			predicates.add(builder.and(builder.equal(root.get(Todo_.URGENCY), todoQuery.getUrgency())));
		}
		// 期限：開始～
		if (!todoQuery.getDeadlineFrom().equals("")) {
			predicates.add(builder.and(builder.greaterThanOrEqualTo(root.get(Todo_.DEADLINE),
					Utils.str2date(todoQuery.getDeadlineFrom()))));
		}
		// ～期限：終了で検索
		if (!todoQuery.getDeadlineTo().equals("")) {
			predicates.add(builder.and(
					builder.lessThanOrEqualTo(root.get(Todo_.DEADLINE), Utils.str2date(todoQuery.getDeadlineTo()))));
		}
		// 完了
		if (todoQuery.getDone() != null && todoQuery.getDone().equals("Y")) {
			predicates.add(builder.and(builder.equal(root.get(Todo_.DONE), todoQuery.getDone())));
		}
		// SELECT 作成
		Predicate[] predArray = new Predicate[predicates.size()];
		predicates.toArray(predArray);
		query = query.select(root).where(predArray).orderBy(builder.asc(root.get(Todo_.id)));
		// クエリ生成
		TypedQuery<Todo> typedQuery = entityManager.createQuery(query);
		// 該当レコード数取得
		int totalRows = typedQuery.getResultList().size();
		// 先頭レコードの位置設定
		typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		// 1 ページ当たりの件数
		typedQuery.setMaxResults(pageable.getPageSize());

		Page<Todo> page = new PageImpl<Todo>(typedQuery.getResultList(), pageable, totalRows);
		// PageImpl(List<T> content,Pageable,long total)
		// content 検索結果（該当ページ分）
		// pageable ページング情報
		// total 検索結果の件数
		//
		// この状態でgetResultListすると、setFirstResultで設定した位置からMaxResultsの件数分の結果を得る

		return page;
	}

}
