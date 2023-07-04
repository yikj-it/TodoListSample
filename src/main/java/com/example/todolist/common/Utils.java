package com.example.todolist.common;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Utils {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public static Date str2date(String s) {
		long ms = 0;
		try {
			ms = sdf.parse(s).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new Date(ms);
	}
}
