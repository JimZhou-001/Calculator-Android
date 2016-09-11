package com.blogspot.jimzhou001.calculator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class CalculatorActivity extends AppCompatActivity {

    //计算结果最多保留8位小数
    final int maxScale = 8;

    //预设的优先级数值最大值（值越大优先级越低），左括号优先级设定为该值
    final static int maxPriorityNum = 5;

    //用哈希表存放运算符及其优先级
    private static HashMap<Character, Integer> operators = new HashMap<Character, Integer>();

    //用于将按钮上的乘除运算符转换为相应的字符
    private static HashMap<String, Character> strToChar = new HashMap<String, Character>();

    //用集合存放数字及小数点
    private static HashSet<Character> Num = new HashSet<Character>();

    //保存中缀表达式
    private static ArrayList<Character> expression = new ArrayList<Character>();

    //依次记录运算数的位数，便于中缀表达式转换为后缀表达式
    private static ArrayList<Integer> digits = new ArrayList<Integer>();

    //k、l用于跟踪运算数，其中k用于输入阶段，l用于计算阶段。m用于计算阶段小数点后的位数处理。
    private static int k = 0,l = 0, m;

    //运算结果转化为字符串
    private static String r;

    //hasPoint用于标记当前输入数据有无小数点，防止一个运算数中出现多个小数点。
    private static boolean hasPoint = false;

    private TextView inputTextView, topTextView;
    private int[] buttonNumId = { R.id.num0, R.id.num1, R.id.num2, R.id.num3, R.id.num4,
            R.id.num5, R.id.num6, R.id.num7, R.id.num8, R.id.num9, R.id.point };//小数点也纳入数字部分
    private int[] buttonOpeId = { R.id.add, R.id.subtract, R.id.multiply, R.id.divide };
    private HashSet<Button> buttonNum = new HashSet<Button>();//便于查找
    private HashSet<Button> buttonOpe = new HashSet<Button>();//此处不含左右括号，注意与哈希表的区分
    private Button c, del, eq, left, right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculator);

        inputTextView = (TextView)findViewById(R.id.inputTextView);
        topTextView = (TextView)findViewById(R.id.topTextView);
        c = (Button)findViewById(R.id.C);
        del = (Button)findViewById(R.id.DEL);
        eq = (Button)findViewById(R.id.equal);
        left = (Button)findViewById(R.id.leftbracket);
        right = (Button)findViewById(R.id.rightbracket);

        digits.add(0);

        ButtonListener buttonListener = new ButtonListener();

        for(int i=0;i<buttonNumId.length;++i)
        {
            buttonNum.add((Button)findViewById(buttonNumId[i]));
        }
        for(Button b : buttonNum)
        {
            b.setOnClickListener(buttonListener);
        }

        for(int i=0;i<buttonOpeId.length;++i)
        {
            buttonOpe.add((Button)findViewById(buttonOpeId[i]));
        }
        for(Button b : buttonOpe)
        {
            b.setOnClickListener(buttonListener);
        }

        c.setOnClickListener(buttonListener);
        del.setOnClickListener(buttonListener);
        eq.setOnClickListener(buttonListener);
        left.setOnClickListener(buttonListener);
        right.setOnClickListener(buttonListener);

        //为运算符哈希表添加元素（运算符）
        operators.put('+', 4);
        operators.put('-', 4);
        operators.put('*', 3);
        operators.put('/', 3);
        operators.put('(', maxPriorityNum);//左括号参与优先级的比较

        strToChar.put("×", '*');
        strToChar.put("÷", '/');

        for(int i=0;i<buttonNumId.length-1;++i)
        {
            Num.add((char)(i+'0'));
        }
        Num.add('.');
    }

    //将中缀表达式转换为后缀表达式
    public static ArrayList<Character> transform(ArrayList<Character> a)
    {
        ArrayList<Character> b = new ArrayList<Character>();
        Stack<Character> stack = new Stack<Character>();//用堆栈保存运算符
        for(int i=0;i<a.size();++i)
        {
            if (Character.isDigit(a.get(i))||a.get(i).equals('.'))
            {
                b.add(a.get(i));
            }
            else if (a.get(i).equals('('))
            {
                stack.push(a.get(i));
            }
            else if (a.get(i).equals(')'))
            {
                while (!(stack.peek().equals('(')))
                {
                    b.add(stack.pop());
                }
                stack.pop();
            }
            else if (operators.containsKey(a.get(i)))
            {
                while ((!stack.empty()) && operators.get(a.get(i)) >= operators.get(stack.peek())) {
                    b.add(stack.pop());
                }
                stack.push(a.get(i));
            }
        }
        while (!(stack.isEmpty()))
            b.add(stack.pop());
        return b;
    }

    //后缀表达式求值
    public static double compute( ArrayList<Character> b)
    {
        int flag = 0;
        double num = 0, temp1, temp2;
        Stack<Double> stack = new Stack<>();//用堆栈保存运算数
        for(int i=0;i<b.size();)
        {
            if (Num.contains(b.get(i)))
            {
                for(int j=0;j<digits.get(l);++j)
                {
                    if (!b.get(i+j).equals('.'))
                    {
                        if (flag==0)
                        {
                            num = num*10+(b.get(i+j)-'0');
                        }
                        else
                        {
                            num = num+(b.get(i+j)-'0')*1.0/Math.pow(10, m++);
                        }
                    }
                    else
                    {
                        flag = 1;
                        m = 1;
                        continue;
                    }
                }
                stack.push(num);
                num = 0;
                i=i+digits.get(l);
                flag = 0;
                ++l;
            }
            else if (operators.containsKey(b.get(i)))
            {
                temp2 = stack.pop();
                temp1 = stack.pop();
                stack.push(result(temp1, b.get(i), temp2));
                ++i;
            }
        }
        return stack.pop();
    }

    //两个操作数的运算
    public static double result(double n1, char c, double n2)
    {
        switch (c)
        {
            case '+': return n1+n2;
            case '-': return n1-n2;
            case '*': return n1*n2;
            case '/': return n1/n2;
        }
        return 0;
    }

    class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            if (expression.isEmpty()) {
                inputTextView.setText("");
            }
            topTextView.setText("");
            if (buttonNum.contains((Button) view)) {
                if (!(((Button) view).getId() ==  buttonNumId[10] && hasPoint)) {
                    if (((Button) view).getId() == buttonNumId[10])
                        hasPoint = true;
                    digits.set(k, digits.get(k) + 1);
                    expression.add(((Button) view).getText().charAt(0));
                    inputTextView.append(((Button) view).getText().toString());
                }
            } else if (buttonOpe.contains((Button) view)) {
                hasPoint = false;
                digits.add(0);
                ++k;
                char c;
                if (strToChar.containsKey(((Button) view).getText()))
                    c = strToChar.get(((Button) view).getText());
                else
                    c = ((Button) view).getText().charAt(0);
                expression.add(c);
                inputTextView.append(((Button) view).getText().toString());
            } else if (left.equals((Button) view) || right.equals((Button) view)) {
                hasPoint = false;
                expression.add(((Button) view).getText().charAt(0));
                inputTextView.append(((Button) view).getText().toString());
            } else if (del.equals((Button) view)) {
                if (!expression.isEmpty()) {
                    if (Num.contains(expression.get(expression.size() - 1))) {
                        if (expression.get(expression.size() - 1).equals('.'))
                            hasPoint = false;
                        digits.set(k, digits.get(k) - 1);
                    }
                    expression.remove(expression.size() - 1);
                    inputTextView.setText(inputTextView.getText().toString().substring(0, inputTextView.getText().toString().length()-1));
                }
                if (expression.isEmpty()) {
                    inputTextView.setText("0");
                }
            } else if(eq.equals((Button)view)||c.equals((Button)view)) {
                if (eq.equals((Button) view)) {
                    try {
                        r = Double.toString(compute(transform(expression)));
                        BigDecimal bd = new BigDecimal(r);
                        r = bd.setScale(maxScale, BigDecimal.ROUND_HALF_UP).toString();

                        if (r.indexOf(".") > 0) {
                            //正则表达
                            r = r.replaceAll("0+?$", "");//去掉后面无用的零
                            r = r.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
                        }

                        //处理类似于1-1等表达式的计算结果
                        if (r.length()>1&&r.charAt(1)=='E'&&r.charAt(0)=='0')
                            r = "0";

                    } catch (Exception e) {
                        r = "ERROR!";
                    } finally {
                        topTextView.setText(inputTextView.getText()+"=");
                        inputTextView.setText(r);
                    }
                } else {
                    inputTextView.setText("0");
                }
                hasPoint = false;
                expression.clear();
                digits.clear();
                digits.add(0);
                k = 0;
                l = 0;
                m = 1;
            }
        }
    }

}