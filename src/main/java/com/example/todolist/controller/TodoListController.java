package com.example.todolist.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.example.todolist.dao.TodoDaoImpl;
import com.example.todolist.entity.Todo;
import com.example.todolist.form.TodoData;
import com.example.todolist.form.TodoQuery;
import com.example.todolist.repository.TodoRepository;
import com.example.todolist.service.TodoService;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
// finalが指定されたフィールドのコンストラクタのみを生成する

public class TodoListController {
	// コンストラクタインジェクションを用いてDIしている
	// ＠AllArgsConstructorでコンストラクタは自動生成される
	// またコンストラクタが一つしかない場合は＠Autowiredは省略できる
	private final TodoRepository todoRepository;
	private final TodoService todoService;
	private final HttpSession session;

	@PersistenceContext
	// EntityManagerはコンストラクタインジェクションと異なるタイミングで作成する
	private EntityManager entityManager;
	TodoDaoImpl todoDaoImpl;

	@PostConstruct
	// このアノテーションを付与すると、コンストラクタの初期化終了後に実施される
	// Daoimplの方ではコンストラクタインジェクションをしているので、
	// ここでは引数でインスタンス生成後のentituManagaerを渡している。
	public void init() {
		todoDaoImpl = new TodoDaoImpl(entityManager);
	}

	@GetMapping("/todo")
	public ModelAndView showTodoList(ModelAndView mv,
			@PageableDefault(page = 0, size = 2, sort = "id") Pageable pageable) {

		// sessionから前回のpageableを取得
		Pageable prevPageable = (Pageable) session.getAttribute("prevPageable");
		if (prevPageable == null) {
			// なければ@PageableDefaultを使う
			prevPageable = pageable;
			session.setAttribute("prevPageable", prevPageable);
		}

		// 一覧を検索して表示する
		mv.setViewName("todoList");
		Page<Todo> todoPage = todoRepository.findAll(pageable);
		mv.addObject("todoPage", todoPage);

		mv.addObject("todoList", todoPage.getContent());
		mv.addObject("todoQuery", new TodoQuery());
		session.setAttribute("todoQuery", new TodoQuery());
		return mv;
	}

	@GetMapping("/todo/query")
	public ModelAndView queryTodo(@PageableDefault(page = 0, size = 2) Pageable pageable, ModelAndView mv) {
		mv.setViewName("todoList");
		// sessionに保存されている条件で検索
		TodoQuery todoQuery = (TodoQuery) session.getAttribute("todoQuery");
		Page<Todo> todoPage = todoDaoImpl.findByCriteria(todoQuery, pageable);
		mv.addObject("todoQuery", todoQuery);
		mv.addObject("todoPage", todoPage);
		mv.addObject("todoList", todoPage.getContent());
		return mv;
	}

	@PostMapping("/todo/query")
	public ModelAndView queryTodo(@ModelAttribute TodoQuery todoQuery, BindingResult result,
			@PageableDefault(page = 0, size = 2) Pageable pageable, // ①
			ModelAndView mv) {

		// 現在のページ位置を保存
		session.setAttribute("prevPageable", pageable);

		mv.setViewName("todoList");
		Page<Todo> todoPage = null; // ②
		if (todoService.isValid(todoQuery, result)) {
			// エラーがなければ検索
			todoPage = todoDaoImpl.findByCriteria(todoQuery, pageable); // ③
			// 入力された検索条件を session に保存
			session.setAttribute("todoQuery", todoQuery); // ④
			mv.addObject("todoPage", todoPage); // ⑤
			mv.addObject("todoList", todoPage.getContent()); // ⑥
		} else {
			// エラーがあった場合検索
			mv.addObject("todoPage", null); // ⑤’
			mv.addObject("todoList", null); // ⑥’
		}
		return mv;
	}

	@PostMapping("/todo/create/form")
	public ModelAndView createTodo(ModelAndView mv) {
		mv.setViewName("todoForm");
		mv.addObject("todoData", new TodoData());
		session.setAttribute("mode", "create");
		return mv;
	}

	@PostMapping("/todo/create/do")
	public String createTodo(@ModelAttribute @Validated TodoData todoData, BindingResult result, Model model) {
		// @ValidatedはtodoDataにバインドされた値をTodoDataクラスのアノテーションでチェック
		// その結果はBindingResultオブジェクトに格納される

		// アノテーションでチェックできないエラーを調査
		boolean isValid = todoService.isValid(todoData, result);
		if (!result.hasErrors() && isValid) {
			// エラーなし
			Todo todo = todoData.toEntity();
			todoRepository.saveAndFlush(todo);
			return redirectPageById();
		} else {
			// エラーあり
			return "todoForm";
		}
	}

	@PostMapping("/todo/cancel")
	public String cancel() {
		return redirectPageById();
	}

	@GetMapping("/todo/{id}")
	public ModelAndView todoById(@PathVariable(name = "id") int id, ModelAndView mv) {
		mv.setViewName("todoForm");
		Todo todo = todoRepository.findById(id).get();
		// findById()の戻り値はoptionalのためgetでTodoを展開している
		mv.addObject("todoData", todo);
		session.setAttribute("mode", "update");
		return mv;
	}

	@PostMapping("/todo/update")
	public String updateTodo(@ModelAttribute @Validated TodoData todoData, BindingResult result, Model model) {
		// エラーチェック
		boolean isUpdate = true;
		boolean isValid = todoService.isValid(todoData, result, isUpdate);
		if (!result.hasErrors() && isValid) {
			// エラーなし
			Todo todo = todoData.toEntity();
			todoRepository.saveAndFlush(todo);
			return redirectPageById();
		} else {
			// エラーあり
			return "todoForm";
		}
	}

	@PostMapping("/todo/delete")
	public String deleteTodo(@ModelAttribute TodoData todoData) {
		todoRepository.deleteById(todoData.getId());
		return redirectPageById();
	}

	private String redirectPageById() {
		Integer pageId = (Integer) session.getAttribute("pageId");

		if (session.getAttribute("pageId") != null) {
			session.setAttribute("pageId", null);
			return "redirect:/todo/query?page=" + (pageId - 1);
		}
		return "redirect:/todo";
	}
}
