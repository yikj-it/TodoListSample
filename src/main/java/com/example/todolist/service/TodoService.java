package com.example.todolist.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.example.todolist.common.Utils;
import com.example.todolist.entity.Todo;
import com.example.todolist.form.TodoData;
import com.example.todolist.form.TodoQuery;
import com.example.todolist.repository.TodoRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TodoService {
	private final TodoRepository todoRepository;

	public boolean isValid(TodoData todoData, BindingResult result) {
		boolean ans = true;
		// 件名が全角スペースだけで構成されていたらエラー
		String title = todoData.getTitle();
		if (title != null && !title.equals("")) {
			boolean isAllDoubleSpace = true;
			for (int i = 0; i < title.length(); i++) {
				if (title.charAt(i) != ' ') {
					isAllDoubleSpace = false;
					break;
				}
			}
			if (isAllDoubleSpace) {
				FieldError fieldError = new FieldError(result.getObjectName(), "title", "件名が全角スペースです");
				result.addError(fieldError);
				ans = false;
			}
		}
		// 期限が過去日付ならエラー
		String deadline = todoData.getDeadline();
		if (!deadline.equals("")) {
			LocalDate tody = LocalDate.now();
			LocalDate deadlineDate = null;
			try {
				deadlineDate = LocalDate.parse(deadline);
				if (deadlineDate.isBefore(tody)) {
					FieldError fieldError = new FieldError(result.getObjectName(), "deadline", "期限を設定するときは今日以降にしてください");
					result.addError(fieldError);
					ans = false;
				}
			} catch (DateTimeException e) {
				FieldError fieldError = new FieldError(result.getObjectName(), "deadline",
						"期限を設定するときは yyyy-mm-dd 形式で入力してください");
				result.addError(fieldError);
				ans = false;
			}
		}
		return ans;
	}

	public boolean isValid(TodoQuery todoQuery, BindingResult result) {
		boolean ans = true;
		// 期限:開始の形式をチェック
		String date = todoQuery.getDeadlineFrom();
		if (!date.equals("")) {
			try {
				LocalDate.parse(date);
			} catch (DateTimeException e) {
				// parse できない場合
				FieldError fieldError = new FieldError(result.getObjectName(), "deadlineFrom",
						"期限：開始を入力するときは yyyy-mm-dd 形式で入力してください");
				result.addError(fieldError);
				ans = false;
			}
		}
		// 期限:終了の形式をチェック
		date = todoQuery.getDeadlineTo();
		if (!date.equals("")) {
			try {
				LocalDate.parse(date);
			} catch (DateTimeException e) {
				// parse できない場合
				FieldError fieldError = new FieldError(result.getObjectName(), "deadlineTo",
						"期限：終了を入力するときは yyyy-mm-dd 形式で入力してください");
				result.addError(fieldError);
				ans = false;
			}
		}
		return ans;
	}

	public List<Todo> doQuery(TodoQuery todoQuery) {
		List<Todo> todoList = null;
		if (todoQuery.getTitle().length() > 0) {
			// タイトルで検索
			todoList = todoRepository.findByTitleLike("%" + todoQuery.getTitle() + "%");
		} else if (todoQuery.getImportance() != null && todoQuery.getImportance() != -1) {
			// 重要度で検索
			todoList = todoRepository.findByImportance(todoQuery.getImportance());
		} else if (todoQuery.getUrgency() != null && todoQuery.getUrgency() != -1) {
			// 緊急度で検索
			todoList = todoRepository.findByUrgency(todoQuery.getUrgency());
		} else if (!todoQuery.getDeadlineFrom().equals("") && todoQuery.getDeadlineTo().equals("")) {
			// 期限 開始～
			todoList = todoRepository
					.findByDeadlineGreaterThanEqualOrderByDeadlineAsc(Utils.str2date(todoQuery.getDeadlineFrom()));
		} else if (todoQuery.getDeadlineFrom().equals("") && !todoQuery.getDeadlineTo().equals("")) {
			// 期限 ～終了
			todoList = todoRepository
					.findByDeadlineLessThanEqualOrderByDeadlineAsc(Utils.str2date(todoQuery.getDeadlineTo()));
		} else if (!todoQuery.getDeadlineFrom().equals("") && !todoQuery.getDeadlineTo().equals("")) {
			// 期限 開始～終了
			todoList = todoRepository.findByDeadlineBetweenOrderByDeadlineAsc(
					Utils.str2date(todoQuery.getDeadlineFrom()), Utils.str2date(todoQuery.getDeadlineTo()));
		} else if (todoQuery.getDone() != null && todoQuery.getDone().equals("Y")) {
			// 完了で検索
			todoList = todoRepository.findByDone("Y");
		} else {
			// 入力条件が無ければ全件検索
			todoList = todoRepository.findAll();
		}
		return todoList;
	}
}
