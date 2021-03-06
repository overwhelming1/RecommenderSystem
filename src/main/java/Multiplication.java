import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Multiplication {
	public static class CooccurrenceMapper extends Mapper<LongWritable, Text, Text, Text> {

		// map method
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			//input: movieB \t movieA=relation

			//pass data to reducer
			String[] line = value.toString().split("\t");
			context.write(new Text(line[0]), new Text(line[1]));
		}
	}

	public static class RatingMapper extends Mapper<LongWritable, Text, Text, Text> {

		// map method
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			//input: user,movie,rating
			//pass data to reducer
			String[] line = value.toString().split(",");
			context.write(new Text(line[1]), new Text(line[0] + ":" + line[2]));
		}
	}

	public static class MultiplicationReducer extends Reducer<Text, Text, Text, DoubleWritable> {
		// reduce method
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			//context.write(new Text("key: " + key), new DoubleWritable(1));

			//key = movieB value = <movieA=relation, movieC=relation... userA:rating, userB:rating...>
			//collect the data for each movie, then do the multiplication
			List<String> movieRelations = new ArrayList<String>();
			List<String> userRatings = new ArrayList<String>();
			for (Text value : values) {
				//context.write(new Text("FUCK"), new DoubleWritable(1));
				//context.write(new Text(key.toString() + value.toString()), new DoubleWritable(1));
				if (value.toString().contains("=")) {
					movieRelations.add(value.toString());
				} else {
					userRatings.add(value.toString());
				}
			}

			//context.write(new Text(String.valueOf(movieRelations.size()) + "and" + String.valueOf(userRatings.size())), new DoubleWritable(3));
			for (String movieRalation : movieRelations) {
				String[] movie = movieRalation.split("=");
				double relation = Double.parseDouble(movie[1]);
				for (String userRating : userRatings) {
					String[] user = userRating.split(":");
					double rating = Double.parseDouble(user[1]);
					context.write(new Text(user[0] + ":" + movie[0]), new DoubleWritable(relation * rating));
				}
			}
		}
	}


	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf);
		job.setJarByClass(Multiplication.class);

		ChainMapper.addMapper(job, CooccurrenceMapper.class, LongWritable.class, Text.class, Text.class, Text.class, conf);
		ChainMapper.addMapper(job, RatingMapper.class, Text.class, Text.class, Text.class, Text.class, conf);

		job.setMapperClass(CooccurrenceMapper.class);
		job.setMapperClass(RatingMapper.class);

		job.setReducerClass(MultiplicationReducer.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);

		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, CooccurrenceMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, RatingMapper.class);

		TextOutputFormat.setOutputPath(job, new Path(args[2]));
		
		job.waitForCompletion(true);
	}
}
