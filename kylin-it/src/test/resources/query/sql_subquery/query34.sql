
  select  (case when '3'='1' then test_cal_dt.week_beg_dt else   test_kylin_fact.cal_dt  end) as xxx ,  sum(price) as sum_price
  from test_kylin_fact
  inner JOIN edw.test_cal_dt as test_cal_dt
  ON test_kylin_fact.cal_dt = test_cal_dt.cal_dt
  inner JOIN test_category_groupings
  ON test_kylin_fact.leaf_categ_id = test_category_groupings.leaf_categ_id AND test_kylin_fact.lstg_site_id = test_category_groupings.site_id
  inner JOIN edw.test_sites as test_sites
  ON test_kylin_fact.lstg_site_id = test_sites.site_id
  
  where case when '3'='1' then test_kylin_fact.cal_dt < date'2012-04-01' else  test_cal_dt.week_beg_dt  > date'2012-04-01' end and lstg_format_name='FP-GTC' 
  
  group by case when '3'='1' then test_cal_dt.week_beg_dt  else   test_kylin_fact.cal_dt end
 
